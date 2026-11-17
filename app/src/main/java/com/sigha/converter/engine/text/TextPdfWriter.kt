package com.sigha.converter.engine.text

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CancellationException

/**
 * Minimal A4 text renderer built on android.graphics.pdf.PdfDocument.
 *
 * Pages are 595 x 842 points (A4 at 72 dpi) with 48 point margins. Line
 * breaking and shaping are done by StaticLayout, so Arabic and other complex
 * scripts are shaped correctly. Paragraphs that do not fit flow onto the next
 * page line by line.
 */
class TextPdfWriter(
    private val expectedParagraphs: Int = 0,
    private val onProgress: (Float) -> Unit = {},
    private val isCancelled: () -> Boolean = { false }
) {

    private data class Look(
        val paint: TextPaint,
        val indent: Float,
        val spaceBefore: Float,
        val spaceAfter: Float,
        val lineSpacingMultiplier: Float,
        val bullet: Boolean = false
    )

    private val document = PdfDocument()
    private val looks: Map<ParagraphStyle, Look> = buildLooks()
    private var page: PdfDocument.Page? = null
    private var pagesWritten = 0
    private var cursorY = TOP
    private var paragraphsPlaced = 0
    private var closed = false

    /**
     * Queues one paragraph; pagination happens as needed.
     *
     * Layout.BREAK_STRATEGY_SIMPLE is the documented value for StaticLayout.Builder; lint
     * expects the newer LineBreaker constant (API 29+), but the numeric value is identical and
     * the builder predates LineBreaker.
     */
    @SuppressLint("WrongConstant")
    fun addParagraph(text: CharSequence, style: ParagraphStyle = ParagraphStyle.BODY) {
        ensureOpen()
        if (isCancelled()) throw CancellationException("Conversion cancelled")
        val look = looks.getValue(style)
        val prepared = if (style == ParagraphStyle.MONO) {
            text.toString().replace("\t", "    ")
        } else {
            text.toString()
        }
        if (prepared.isEmpty()) {
            addSpacing(look.paint.textSize * 1.2f)
            advance()
            return
        }
        val width = (CONTENT_WIDTH - look.indent).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(prepared, 0, prepared.length, look.paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, look.lineSpacingMultiplier)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        place(layout, look)
        advance()
    }

    /** Writes every page to [outputFile]; the writer cannot be reused afterwards. */
    fun finish(outputFile: File) {
        ensureOpen()
        try {
            finishPage()
            if (pagesWritten == 0) {
                startPage()
                finishPage()
            }
            outputFile.parentFile?.mkdirs()
            BufferedOutputStream(FileOutputStream(outputFile), BUFFER_SIZE).use { output ->
                document.writeTo(output)
            }
        } finally {
            closed = true
            runCatching { document.close() }
        }
        onProgress(1f)
    }

    /** Releases native resources when a conversion fails before [finish]. */
    fun close() {
        if (closed) return
        closed = true
        runCatching { document.close() }
    }

    private fun place(layout: StaticLayout, look: Look) {
        if (page == null) startPage()
        val lineCount = layout.lineCount
        if (lineCount == 0) {
            addSpacing(look.paint.textSize * 1.2f)
            return
        }
        val firstLineHeight = (layout.getLineBottom(0) - layout.getLineTop(0)).toFloat()
        if (cursorY + look.spaceBefore + firstLineHeight > BOTTOM && cursorY > TOP + 1f) {
            finishPage()
            startPage()
        }
        cursorY += look.spaceBefore
        var first = 0
        while (first < lineCount) {
            if (isCancelled()) throw CancellationException("Conversion cancelled")
            val sliceTop = layout.getLineTop(first).toFloat()
            val available = BOTTOM - cursorY
            var last = first
            while (last < lineCount && (layout.getLineBottom(last) - sliceTop) <= available) {
                last++
            }
            if (last == first) {
                if (cursorY > TOP + 1f) {
                    finishPage()
                    startPage()
                    continue
                }
                last = first + 1
            }
            drawSlice(layout, look, first, last)
            cursorY += layout.getLineBottom(last - 1) - sliceTop
            first = last
            if (first < lineCount) {
                finishPage()
                startPage()
            }
        }
        cursorY += look.spaceAfter
    }

    private fun drawSlice(layout: StaticLayout, look: Look, firstLine: Int, lastLine: Int) {
        val current = page ?: return
        val canvas = current.canvas
        canvas.save()
        canvas.clipRect(LEFT, TOP, LEFT + CONTENT_WIDTH, BOTTOM)
        canvas.translate(LEFT + look.indent, cursorY - layout.getLineTop(firstLine))
        layout.draw(canvas)
        canvas.restore()
        if (look.bullet && firstLine == 0) {
            val baseline = cursorY + (layout.getLineBaseline(0) - layout.getLineTop(0))
            canvas.drawText(BULLET, LEFT, baseline, looks.getValue(ParagraphStyle.BODY).paint)
        }
    }

    private fun startPage() {
        val info = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH.toInt(),
            PAGE_HEIGHT.toInt(),
            pagesWritten + 1
        ).create()
        page = document.startPage(info)
        cursorY = TOP
    }

    private fun finishPage() {
        val current = page ?: return
        document.finishPage(current)
        page = null
        pagesWritten++
    }

    private fun addSpacing(height: Float) {
        if (height <= 0f) return
        if (cursorY + height > BOTTOM && cursorY > TOP + 1f) {
            finishPage()
            startPage()
        }
        cursorY += height
    }

    private fun advance() {
        paragraphsPlaced++
        if (expectedParagraphs > 0) {
            onProgress((paragraphsPlaced.toFloat() / expectedParagraphs).coerceIn(0f, 0.98f))
        }
    }

    private fun ensureOpen() {
        check(!closed) { "The PDF writer is already closed." }
    }

    private fun buildLooks(): Map<ParagraphStyle, Look> {
        fun paint(size: Float, style: Int, mono: Boolean): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
                typeface = if (mono) {
                    Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                } else {
                    Typeface.create(Typeface.SANS_SERIF, style)
                }
            }

        return mapOf(
            ParagraphStyle.BODY to Look(paint(11f, Typeface.NORMAL, false), 0f, 0f, 6f, 1.45f),
            ParagraphStyle.H1 to Look(paint(20f, Typeface.BOLD, false), 0f, 14f, 8f, 1.25f),
            ParagraphStyle.H2 to Look(paint(16f, Typeface.BOLD, false), 0f, 12f, 6f, 1.3f),
            ParagraphStyle.H3 to Look(paint(13f, Typeface.BOLD, false), 0f, 9f, 4f, 1.35f),
            ParagraphStyle.BULLET to Look(paint(11f, Typeface.NORMAL, false), 14f, 0f, 3f, 1.4f, true),
            ParagraphStyle.MONO to Look(paint(8.5f, Typeface.NORMAL, true), 0f, 0f, 1f, 1.25f),
            ParagraphStyle.QUOTE to Look(paint(10.5f, Typeface.ITALIC, false), 18f, 0f, 6f, 1.4f)
        )
    }

    private companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 48f
        const val LEFT = MARGIN
        const val TOP = MARGIN
        const val BOTTOM = PAGE_HEIGHT - MARGIN
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val BUFFER_SIZE = 64 * 1024
        const val BULLET = "\u2022"
    }
}

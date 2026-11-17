package com.sigha.converter.engine.text

/** Paragraph styles shared by the text renderer and the document readers. */
enum class ParagraphStyle { BODY, H1, H2, H3, BULLET, MONO, QUOTE }

/** One rendered paragraph. Kept plain so it can travel between engines and tests. */
data class TextParagraph(
    val text: String,
    val style: ParagraphStyle = ParagraphStyle.BODY
)

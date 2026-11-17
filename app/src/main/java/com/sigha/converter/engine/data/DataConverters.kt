package com.sigha.converter.engine.data

import android.app.Application
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionRequest
import com.sigha.converter.core.ConversionResult
import com.sigha.converter.core.Converter
import com.sigha.converter.core.FileFormat
import com.sigha.converter.core.Note
import com.sigha.converter.engine.text.EngineFormats
import com.sigha.converter.engine.text.TextIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.util.concurrent.CancellationException

/** Tabular data converters: CSV/TSV to each other and to/from JSON. */
fun dataConverters(app: Application): List<Converter> = listOf(
    CsvToTsv(),
    TsvToCsv(),
    CsvToJson(),
    JsonToCsvConverter()
)

private class CsvToTsv : Converter {
    override val id: String = "data-csv-to-tsv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("tsv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            convertDelimited(request, ',', '\t', "\n")
        }
}

private class TsvToCsv : Converter {
    override val id: String = "data-tsv-to-csv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("tsv"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            convertDelimited(request, '\t', ',', "\r\n")
        }
}

private class CsvToJson : Converter {
    override val id: String = "data-csv-to-json"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("json"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            val rows = TextIo.open(request.context, request.input).use { source ->
                openWriter(request).use { writer ->
                    val count = JsonCsv.csvToJson(Csv.Parser(source.reader, ','), writer) { done ->
                        if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                        if (done % 512 == 0) request.onProgress(source.progress())
                    }
                    writer.flush()
                    count
                }
            }
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                if (rows == 0) Note.CsvHeaderOnly else Note.JsonValuesAsStrings
            )
        }
}

private class JsonToCsvConverter : Converter {
    override val id: String = "data-json-to-csv"
    override val inputs: Set<FileFormat> = setOf(EngineFormats.of("json"))
    override val outputs: Set<FileFormat> = setOf(EngineFormats.of("csv"))

    override suspend fun convert(request: ConversionRequest): ConversionResult =
        withContext(Dispatchers.IO) {
            request.onProgress(0f)
            val text = readInput(request)
            request.onProgress(0.4f)
            val rows = openWriter(request).use { writer ->
                val count = JsonCsv.jsonToCsv(text, writer)
                writer.flush()
                count
            }
            request.onProgress(1f)
            ConversionResult(
                listOf(request.outputFile),
                Note.JsonObjectsExported(rows)
            )
        }
}

private fun convertDelimited(
    request: ConversionRequest,
    sourceDelimiter: Char,
    targetDelimiter: Char,
    lineEnding: String
): ConversionResult {
    var rows = 0
    TextIo.open(request.context, request.input).use { source ->
        openWriter(request).use { writer ->
            val parser = Csv.Parser(source.reader, sourceDelimiter)
            while (true) {
                if (request.isCancelled()) throw CancellationException("Conversion cancelled")
                val row = parser.readRow() ?: break
                Csv.appendRow(writer, row, targetDelimiter, lineEnding)
                rows++
                if (rows % 256 == 0) request.onProgress(source.progress())
            }
            writer.flush()
        }
    }
    if (rows == 0) throw ConversionException("The file has no rows to convert.")
    request.onProgress(1f)
    return ConversionResult(listOf(request.outputFile))
}

private fun openWriter(request: ConversionRequest): BufferedWriter {
    request.outputFile.parentFile?.mkdirs()
    val stream = BufferedOutputStream(request.outputFile.outputStream(), TextIo.BUFFER_SIZE)
    return TextIo.writer(stream)
}

private fun readInput(request: ConversionRequest): String {
    val stream = request.context.contentResolver.openInputStream(request.input)
        ?: throw ConversionException("Cannot read the selected file.")
    return stream.use { TextIo.readText(it) }
}

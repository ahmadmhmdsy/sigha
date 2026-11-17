package com.sigha.converter.engine.pdf

import android.app.Application
import com.sigha.converter.core.Converter

/** PDF engine edges: PDF -> PNG/JPG (PdfRenderer) and PDF -> TXT (PDFBox). */
fun pdfConverters(app: Application): List<Converter> = listOf(
    PdfToImageConverter(app.applicationContext),
    PdfToTextConverter(app.applicationContext)
)

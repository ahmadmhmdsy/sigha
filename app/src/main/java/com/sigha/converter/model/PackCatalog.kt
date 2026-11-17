package com.sigha.converter.model

/**
 * Every pack the app can download. Nothing is fetched until the user taps Download.
 *
 * Each entry is verified by hand: the URL is pinned to a commit (or to a versioned release)
 * and the SHA-256 is computed from the exact bytes that URL serves. Re-verify both before
 * changing anything here, and keep docs/models/integration.md in sync.
 */
object PackCatalog {

    /** tessdata_best at commit e12c65a; Apache-2.0. */
    private const val TESSDATA_BEST =
        "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/e12c65a915945e4c28e237a9b52bc4a8f39a0cec"

    private val ENG = PackFile(
        fileName = "eng.traineddata",
        url = "$TESSDATA_BEST/eng.traineddata",
        sha256 = "8280aed0782fe27257a68ea10fe7ef324ca0f8d85bd2fd145d1c2b560bcb66ba",
        sizeBytes = 15_400_601L
    )

    private val ARA = PackFile(
        fileName = "ara.traineddata",
        url = "$TESSDATA_BEST/ara.traineddata",
        sha256 = "ab9d157d8e38ca00e7e39c7d5363a5239e053f5b0dbdb3167dde9d8124335896",
        sizeBytes = 12_603_724L
    )

    val OCR = ModelPack(
        id = ModelPacks.OCR_PACK_ID,
        title = "OCR: English + Arabic",
        description = "Reads text from photos and scanned PDFs in English and Arabic. " +
            "Downloads once, then needs no network.",
        license = "Apache-2.0",
        licenseUrl = "https://github.com/tesseract-ocr/tessdata_best/blob/main/LICENSE",
        files = listOf(ENG, ARA),
        subdirectory = "tessdata"
    )

    val ALL: List<ModelPack> = listOf(OCR)

    fun byId(id: String): ModelPack? = ALL.firstOrNull { it.id == id }
}

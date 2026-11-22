package com.sigha.converter.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputDestinationTest {

    @Test
    fun `image targets go to the gallery collection`() {
        assertEquals(OutputDestination.IMAGES, destinationFor(FormatCategory.IMAGE, "image/jpeg"))
        assertEquals(OutputDestination.IMAGES, destinationFor(FormatCategory.IMAGE, "image/png"))
        assertEquals(OutputDestination.IMAGES, destinationFor(FormatCategory.IMAGE, "image/gif"))
    }

    @Test
    fun `video targets go to movies`() {
        assertEquals(OutputDestination.VIDEO, destinationFor(FormatCategory.VIDEO, "video/mp4"))
        assertEquals(OutputDestination.VIDEO, destinationFor(FormatCategory.VIDEO, ""))
    }

    @Test
    fun `audio targets go to music`() {
        assertEquals(OutputDestination.AUDIO, destinationFor(FormatCategory.AUDIO, "audio/mpeg"))
        assertEquals(OutputDestination.AUDIO, destinationFor(FormatCategory.AUDIO, ""))
    }

    @Test
    fun `documents and everything else stay in downloads`() {
        assertEquals(OutputDestination.DOWNLOADS, destinationFor(FormatCategory.PDF, "application/pdf"))
        assertEquals(OutputDestination.DOWNLOADS, destinationFor(FormatCategory.TEXT, "text/plain"))
        assertEquals(OutputDestination.DOWNLOADS, destinationFor(FormatCategory.ARCHIVE, "application/zip"))
        assertEquals(OutputDestination.DOWNLOADS, destinationFor(FormatCategory.SUBTITLE, "text/vtt"))
        assertEquals(OutputDestination.DOWNLOADS, destinationFor(FormatCategory.SPREADSHEET, ""))
    }

    @Test
    fun `mime wins when the category is generic`() {
        assertEquals(OutputDestination.IMAGES, destinationFor(FormatCategory.DATA, "image/webp"))
        assertEquals(OutputDestination.AUDIO, destinationFor(FormatCategory.DATA, "audio/ogg"))
        assertEquals(OutputDestination.VIDEO, destinationFor(FormatCategory.DATA, "video/webm"))
    }

    @Test
    fun `folders keep their public path and app subfolder`() {
        assertEquals("Pictures/Sigha", OutputDestination.IMAGES.folderName)
        assertEquals("Music/Sigha", OutputDestination.AUDIO.folderName)
        assertEquals("Movies/Sigha", OutputDestination.VIDEO.folderName)
        assertEquals("Download/Sigha", OutputDestination.DOWNLOADS.folderName)
        assertEquals("Pictures", OutputDestination.IMAGES.legacyDirectory)
        assertEquals("Download", OutputDestination.DOWNLOADS.legacyDirectory)
    }
}

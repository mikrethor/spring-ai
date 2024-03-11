package com.xavierbouclet.springai

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun String.decodeFromBase64(base64EncodedString: String): ByteArray =
    Base64.getDecoder().decode(base64EncodedString.toByteArray(StandardCharsets.UTF_8))

object ZipUtilities {
    fun createZipFile(pngFiles: Map<String, ByteArray>): ByteArray {
        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                pngFiles.forEach { (fileName, fileContent) ->
                    val entry = ZipEntry(fileName)
                    zos.putNextEntry(entry)
                    zos.write(fileContent)
                    zos.closeEntry()
                }
            }
            return baos.toByteArray()
        }
    }

}


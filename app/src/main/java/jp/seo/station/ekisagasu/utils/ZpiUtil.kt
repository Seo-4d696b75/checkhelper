package jp.seo.station.ekisagasu.utils

import java.io.File
import java.util.zip.ZipFile

fun unzip(src: File, dstDir: File) {
    val zipFile = ZipFile(src)
    val entries = zipFile.entries()
    while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        val file = File(dstDir, entry.name)
        if (entry.isDirectory) {
            file.mkdir()
        } else {
            zipFile.getInputStream(entry).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

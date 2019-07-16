package com.accforaus.libhwp.server.utils

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.io.*
import java.lang.Exception
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

@Component
class Compressor {
    companion object {
        val logger = LoggerFactory.getLogger(Compressor::class.java)
    }

    fun zip(src: File, destDir: File = src) : String {
        return zip(src, destDir, Charset.defaultCharset().name(), true)
    }

    fun zip(src: File, includeSrc: Boolean) {
        zip(src, Charset.defaultCharset().name(), includeSrc)
    }

    fun zip(src: File, charsetName: String, includeSrc: Boolean) {
        zip(src, src.parentFile, charsetName, includeSrc)
    }

    fun zip(src: File, os: OutputStream) {
        zip (src, os, Charset.defaultCharset().name(), true)
    }

    fun zip(src: File, destDir: File, charsetName: String, includeSrc: Boolean) : String {
        var filename = src.name
        if (!src.isDirectory) {
            val pos = filename.lastIndexOf(".")
            if (pos > 0) filename = filename.substring(0, pos)
        }
        filename += ".zip"
        filename = SimpleDateFormat("yyyy-MM-dd_hh-mm-ss'.zip'").format(Date())
        ensureDestDir(destDir)

        val zippedFile = File(destDir, filename)
        try {
            if (!zippedFile.exists()) zippedFile.createNewFile()
            zip(src, FileOutputStream(zippedFile), charsetName, includeSrc)
        } catch (ex: Exception) {
            logger.error(ex.message)
        }

        return filename
    }

    fun zip(src: File, os: OutputStream, charsetName: String, includeSrc: Boolean) {
        val zos = ZipArchiveOutputStream(os)
        zos.encoding = charsetName
        var fis: FileInputStream? = null
        var ze: ZipArchiveEntry? = null

        var length = 0
        val buf = ByteArray(8 * 1024)
        var name = ""
        val stack = Stack<File>()
        var root: File? = null

        if (src.isDirectory) {
            if (includeSrc) {
                stack.push(src)
                root = src.parentFile
            } else {
                for (file in src.listFiles()) stack.push(file)
                root = src
            }
        } else {
            stack.push(src)
            root = src.parentFile
        }
        while (!stack.isEmpty()) {
            val f = stack.pop()
            name = toPath(root!!, f)
            if (f.isDirectory) {
                for (file in f.listFiles()) {
                    if (file.isDirectory) stack.push(file)
                    else stack.add(0, file)
                }
            } else {
                ze = ZipArchiveEntry(name)
                try {
                    zos.putArchiveEntry(ze)
                    fis = FileInputStream(f)
                    while (true) {
                        length = fis.read(buf, 0, buf.size)
                        if (length >= 0) {
                            zos.write(buf, 0, length)
                        } else {
                            break
                        }
                    }
                    fis.close()
                    zos.closeArchiveEntry()
                } catch (ex: IOException) {
                    logger.error(ex.message)
                }
            }
        }
        try {
            zos.close()
        } catch (ex: IOException) {
            logger.error(ex.message)
        }
    }

    private fun toPath(root: File, dir: File) : String {
        var path = dir.absolutePath
        path = path.substring(root.absolutePath.length).replace(File.separatorChar, '/')
        if (path.startsWith("/")) path = path.substring(1)
        if (dir.isDirectory && !path.endsWith("/")) path += "/"
        return path
    }

    private fun ensureDestDir(dir: File) {
        if (!dir.exists()) dir.mkdirs()
    }
}
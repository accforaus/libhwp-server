package com.accforaus.libhwp.server.service.impl

import com.google.gson.internal.LinkedTreeMap
import com.accforaus.libhwp.server.domain.FileInfo
import com.accforaus.libhwp.server.domain.ImageInfo
import com.accforaus.libhwp.server.exception.FileStorageException
import com.accforaus.libhwp.server.exception.MyFileNotFoundException
import com.accforaus.libhwp.server.property.FileDirectoryProperty
import com.accforaus.libhwp.server.service.ActiveFileService
import com.accforaus.libhwp.server.service.FileService
import com.accforaus.libhwp.server.utils.Compressor
import com.accforaus.libhwp.server.utils.DirectorySearcher
import com.tang.hwplib.objects.HWPDocument
import com.tang.hwplib.objects.bindata.HWPEmbeddedBinaryData
import com.tang.hwplib.objects.bodytext.paragraph.HWPParagraph
import com.tang.hwplib.tools.table.HWPTable
import com.tang.hwplib.tools.table.getTableText
import com.tang.hwplib.tools.textextractor.HWPTextExtractType
import com.tang.hwplib.tools.textextractor.getNormalString
import javafx.scene.Parent
import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.ImageReadException
import org.apache.commons.imaging.Imaging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
class FileServiceImpl @Autowired constructor(fileDirectoryProperty: FileDirectoryProperty, val compressor: Compressor, val activeFileService: ActiveFileService, val directorySearcher: DirectorySearcher) : FileService {
    private var fileStorageLocationMap = HashMap<String, Path>()
    private lateinit var binPath: String

    init {
        fileDirectoryProperty.dir.forEach { key: String, path: String ->
            fileStorageLocationMap[key] = Paths.get(path).toAbsolutePath().normalize()
        }
        fileStorageLocationMap.forEach { t: String, u: Path ->
            try {
                Files.createDirectories(u)
            } catch (e: Exception) {
                throw FileStorageException("Could not create the directory where the uploaded files will be stored", e)
            }
        }
    }

    override fun storeFile(file: MultipartFile): String {
        val filename = StringUtils.cleanPath(file.originalFilename ?: "")
        try {
            if (filename.contains("..")) throw FileStorageException("Sorry! Filename contains invalid path sequence $filename")
            val targetLocation = fileStorageLocationMap["temp"]!!.resolve(filename)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            return filename
        } catch (e: IOException) {
            throw FileStorageException("Could not store file $filename. Please try again!", e)
        }
    }

    override fun storeHWPFile(file: MultipartFile): HashMap<String, String> {
        val filename = StringUtils.cleanPath(file.originalFilename ?: "")
        try {
            if (filename.contains("..")) throw FileStorageException("Sorry! Filename contains invalid path sequence $filename")
            val targetLocation = fileStorageLocationMap["temp"]!!.resolve(filename)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            val hwp = HWPDocument(targetLocation.toString())
            return activeFileService.addActiveFile(hwp)
        } catch (ex: IOException) {
            throw FileStorageException("Could not store File $filename, Please try again!")
        }
    }

    override fun loadFileAsResource(filename: String): Resource {
        try {
            val filePath = this.fileStorageLocationMap["temp"]!!.resolve(activeFileService.getActiveFileByName(filename.toInt())!!.toFile().name).normalize()
            val resource = UrlResource(filePath.toUri())
            if (resource.exists()) return resource
            else
                throw MyFileNotFoundException("File not found $filename")
        } catch (ex: MalformedURLException) {
            throw MyFileNotFoundException("File not found $filename", ex)
        }
    }

    override fun loadZipAsResource(filename: String): Resource {
        try {
            val filePath = this.fileStorageLocationMap["compress"]!!.resolve(filename).normalize()
            val resource = UrlResource(filePath.toUri())
            if (resource.exists()) return resource
            else
                throw MyFileNotFoundException("File not found $filename")
        } catch (ex: MalformedURLException) {
            throw MyFileNotFoundException("File not found $filename", ex)
        }
    }

    override fun loadHwpAsResource(filename: String): Resource {
        try {
            val filePath = fileStorageLocationMap["temp"]!!.resolve(filename).normalize()
            val resource = UrlResource(filePath.toUri())
            if (resource.exists()) return resource
            else
                throw MyFileNotFoundException("File not found $filename")
        } catch (ex: MalformedURLException) {
            throw MyFileNotFoundException("File not found $filename", ex)
        }
    }
    override fun getFileNameChanged(data: LinkedTreeMap<String, String>): String {
        for (key in data.keys) {
            activeFileService.getActiveFileByName(key.toInt())?.run {
                this.write(fileStorageLocationMap["save"]!!.resolve(data[key]).toString())
            }
        }
        val filename = compress(fileStorageLocationMap["save"]!!)
        cleanSaveDir()
        return filename
    }

    override fun getBinPath(): String = binPath

    override fun getCompressedFileSrc(): Path {
        return fileStorageLocationMap["save"]!!.resolve("complete.zip")
    }

    private fun compress(path: Path) = compressor.zip(path.toFile(), fileStorageLocationMap["compress"]!!.toFile())

    private fun cleanSaveDir() {
        val saveDir = fileStorageLocationMap["save"]!!.toFile()
        saveDir.listFiles().forEach { it.delete() }
    }

    private fun cleanImageDirByIndex(path: Path) {
        if (!path.toFile().exists()) path.toFile().mkdir()
        else {
            if (path.toFile().isDirectory) {
                path.toFile().listFiles().forEach { it.delete() }
            } else {
                path.toFile().delete()
                cleanImageDirByIndex(path)
            }
        }
    }

    override fun extractImage(index: Int) : ArrayList<ImageInfo> {
        return extract(index)
    }

    private fun extract(index: Int) : ArrayList<ImageInfo> {
        val imageList = ArrayList<ImageInfo>()
        activeFileService.getActiveFileByName(index)?.let {
            val targetPath = fileStorageLocationMap["images"]!!.resolve(index.toString())
            cleanImageDirByIndex(targetPath)
            val savePath = targetPath.toFile().absolutePath
            it.binData.embeddedBinaryDataList.forEach { binData: HWPEmbeddedBinaryData ->
                run {
                    var hasError = false
                    var name = binData.name
                    val fos = FileOutputStream("$savePath/$name")
                    try {
                        fos.write(binData.data)
                        if (name.contains("pcx") || name.contains("PCX")) {
                            try {
                                val imageFile = File("$savePath/$name")
                                val image = Imaging.getBufferedImage(imageFile)
                                name = if (name.contains("pcx")) name.replace("pcx", "png") else name.replace("PCX", "png")
                                val newImage = File("$savePath/$name")
                                Imaging.writeImage(image, newImage, ImageFormats.PNG, HashMap())
                                imageFile.delete()
                            } catch (ex: ImageReadException) {
                                ex.printStackTrace()
                                hasError = true
                            }
                        }
                        if (!hasError) {
                            val imageInfo = ImageInfo().apply {
                                this.name = name
                                this.size = binData.data?.size ?: 0
                            }
                            imageList.add(imageInfo)
                        } else hasError = false
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        fos.close()
                    }
                }
            }
        }
        return imageList
    }

    override fun getNormalText(fileIndex: Int, type: HWPTextExtractType): String {
        activeFileService.getActiveFileByName(fileIndex)?.run {
            return this.getNormalString(type)
        } ?: return ""
    }

    override fun getTable(fileIndex: Int): ArrayList<HWPTable> {
        activeFileService.getActiveFileByName(fileIndex)?.run {
            return this.getTableText()
        } ?: return ArrayList()
    }

    override fun attachFile(data: LinkedTreeMap<String, String>): String {
        activeFileService.getActiveFileByName(data["main"]!!.toInt())?.run {
            for (key in data.keys) {
                if (key != "main") {
                    activeFileService.getActiveFileByName(data[key]!!.toInt())?.let { this.plus(it) }
                }
            }
            val targetPath = fileStorageLocationMap["temp"]!!.resolve("complete.hwp")
            this.write(targetPath.toAbsolutePath().toString())
            return "complete.hwp"
        }
        return ""
    }

    override fun searchDirectory(path: String): ArrayList<FileInfo> {
        return directorySearcher.search(path)
    }
}
package com.accforaus.libhwp.server.service

import com.google.gson.internal.LinkedTreeMap
import com.accforaus.libhwp.server.domain.FileInfo
import com.accforaus.libhwp.server.domain.ImageInfo
import com.tang.hwplib.objects.HWPDocument
import com.tang.hwplib.tools.table.HWPTable
import com.tang.hwplib.tools.textextractor.HWPTextExtractType
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path


interface FileService {
    fun storeFile(file: MultipartFile) : String
    fun storeHWPFile(file: MultipartFile) : HashMap<String, String>
    fun loadFileAsResource(filename: String) : Resource
    fun loadZipAsResource(filename: String) : Resource
    fun loadHwpAsResource(filename: String) : Resource
    fun getBinPath() : String
    fun getNormalText(fileIndex: Int, type: HWPTextExtractType) : String
    fun getTable(fileIndex: Int) : ArrayList<HWPTable>
    fun attachFile(data: LinkedTreeMap<String, String>) : String
    fun getCompressedFileSrc() : Path
    fun getFileNameChanged(data: LinkedTreeMap<String, String>) : String
    fun extractImage(index: Int) : ArrayList<ImageInfo>
    fun searchDirectory(path: String) : ArrayList<FileInfo>
}
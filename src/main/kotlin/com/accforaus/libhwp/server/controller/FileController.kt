package com.accforaus.libhwp.server.controller

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.accforaus.libhwp.server.domain.FileInfo
import com.accforaus.libhwp.server.domain.ImageInfo
import com.accforaus.libhwp.server.service.FileService
import com.tang.hwplib.tools.table.HWPTable
import com.tang.hwplib.tools.textextractor.HWPTextExtractType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@RestController
class FileController(@Autowired val gson: Gson) {
    companion object {
        val logger = LoggerFactory.getLogger(FileController::class.java)!!
    }

    @Autowired
    private lateinit var fileService: FileService

    @CrossOrigin
    @PostMapping("/api/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile) : HashMap<String, String> {
        val fileInfo = fileService.storeHWPFile(file)
        fileInfo["download"] = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(fileInfo["index"]!!).toUriString()
        return fileInfo
    }


    @CrossOrigin
    @PostMapping("/api/filename")
    fun getAttachedFile(@RequestParam data: String) : String {
        val dataMap = gson.fromJson(data, Map::class.java)
        val filename = fileService.getFileNameChanged(dataMap as LinkedTreeMap<String, String>)
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/download/zip/").path(filename).toUriString()
    }

    @CrossOrigin
    @PostMapping("/api/upload/multiple")
    fun uploadMultipleFiles(@RequestParam("files") files: Array<MultipartFile>) : List<HashMap<String, String>> {
        return files.map { uploadFile(it) }
    }

    @CrossOrigin
    @PostMapping("/api/attach")
    fun attachFiles(@RequestParam data: String) : String {
        val dataMap = gson.fromJson(data, Map::class.java)
        val filename = fileService.attachFile(dataMap as LinkedTreeMap<String, String>)
        return if (filename !== "")
            ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/download/hwp/").path(filename).toUriString()
        else ""
    }

    @CrossOrigin
    @GetMapping("/api/image/{index}")
    fun extractImages(@PathVariable index: Int) : ArrayList<ImageInfo> {
        val imageList = fileService.extractImage(index)
        imageList.forEach {
            it.path = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/images/images/$index/${it.name}")
                    .toUriString()
        }
        return imageList
    }

    @CrossOrigin
    @ResponseBody
    @GetMapping("/api/text/{index}", produces = ["application/text;charset=utf-8"])
    fun getNormalString(@PathVariable index: Int) : String {
        val jsonObject = JsonObject()
        jsonObject.addProperty("texts", fileService.getNormalText(index, HWPTextExtractType.All))
        return gson.toJson(jsonObject)
    }

    @CrossOrigin
    @GetMapping("/api/table/{index}")
    fun getTable(@PathVariable index: Int) : ArrayList<HWPTable> {
        return fileService.getTable(index)
    }

    @CrossOrigin
    @GetMapping("/api/download/zip/{filename:.+}")
    fun downloadZipFile(@PathVariable filename: String, request: HttpServletRequest) : ResponseEntity<Resource> {
        val resource = fileService.loadZipAsResource(filename)
        return getResponseEntityByResource(resource, request)
    }

    @CrossOrigin
    @GetMapping("/api/download/hwp/{filename:.+}")
    fun downloadHwpFile(@PathVariable filename: String, request: HttpServletRequest) : ResponseEntity<Resource> {
        val resource = fileService.loadHwpAsResource(filename)
        return getResponseEntityByResource(resource, request)
    }

    @CrossOrigin
    @GetMapping("/api/downloadFile/{filename}")
    fun downloadFile(@PathVariable filename: String, request: HttpServletRequest) : ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource(filename)
        return getResponseEntityByResource(resource, request)
    }

    @CrossOrigin
    @PostMapping("/api/search")
    fun searchDirectory(@RequestParam path: String) : ArrayList<FileInfo> = fileService.searchDirectory(path)

    private fun getResponseEntityByResource(resource: Resource, request: HttpServletRequest) : ResponseEntity<Resource> {
        var contentType: String? = null

        try {
            contentType = request.servletContext.getMimeType(resource.file.absolutePath)
        } catch (ex: IOException) {
            logger.error("Could not determine file type")
        }

        if (contentType == null) contentType = "application/octet-stream"

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
                .body(resource)
    }
}
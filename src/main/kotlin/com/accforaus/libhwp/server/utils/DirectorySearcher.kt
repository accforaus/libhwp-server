package com.accforaus.libhwp.server.utils

import com.accforaus.libhwp.server.domain.FileInfo
import com.tang.hwplib.objects.HWPDocument
import com.tang.hwplib.tools.textextractor.HWPTextExtractType
import com.tang.hwplib.tools.textextractor.getNormalString
import org.apache.poi.hdgf.extractor.VisioTextExtractor
import org.apache.poi.hpbf.extractor.PublisherTextExtractor
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Component
class DirectorySearcher {
    fun search(path: String) : ArrayList<FileInfo> = Files.walk(Paths.get(path)).parallel()
            .filter { !it.toFile().isDirectory && it.toFile().absolutePath.contains(".") }
            .map {
                FileInfo().apply {
                    filename = it.toFile().name
                    directory = it.toFile().absolutePath.replace(filename, "")
                    content = read(it.toFile().absolutePath)
                    extension = getExtension(it.toFile().absolutePath).orElse("null")
                    size = (it.toFile().length() / 1024).toInt()
                }
            }.collect(Collectors.toList()) as ArrayList<FileInfo>

    private fun getExtension(filename: String) : Optional<String> = Optional.ofNullable(filename)
            .filter { it.contains(".") }.map { it.substring(filename.lastIndexOf(".") + 1) }

    private fun hasExtension(path: Path) : Boolean = when (path.toFile().absolutePath.indexOf(".")) {
        in 0..Int.MAX_VALUE -> true
        else -> false
    }

    private fun read(absolutePath: String) : String {
        if (absolutePath.contains(".hwp", true)) return readHwp(absolutePath)
        if (absolutePath.contains(".doc", true)) return readWord(absolutePath)
        if (absolutePath.contains(".xls", true)) return readExcel(absolutePath)
        if (absolutePath.contains(".pptx", true)) return readPowerPoints(absolutePath)
        if (absolutePath.contains(".vsd", true)) return readVisio(absolutePath)
        return if (absolutePath.contains(".pub", true)) readPublisher(absolutePath)
        else readNormal(absolutePath)
    }

    private fun readHwp(path: String) : String = try {
        HWPDocument(path).getNormalString(HWPTextExtractType.All)
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readWord(path: String) : String
            = if (path.contains("docx", true)) readDocx(path) else readDoc(path)

    private fun readDocx(path: String) : String = try {
        XWPFWordExtractor(XWPFDocument(FileInputStream(path))).text
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readDoc(path: String) : String = try {
        WordExtractor(HWPFDocument(FileInputStream(path))).text
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readPowerPoints(path: String) : String  = try {
        val pptxShow = XMLSlideShow(FileInputStream(path))

        val slides = pptxShow.slides

        slides.joinToString(" ") { slide ->
            val notes = slide.notes
            notes.map { shape ->
                if (shape is XSLFTextShape)
                    shape.map { it.text }
            }.joinToString(" ")
        }
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readExcel(path: String) : String
            = if (path.contains(".xlsx")) readXlsx(path) else readXls(path)

    private fun readXlsx(path: String) : String = try {
        val excel = XSSFWorkbook(FileInputStream(path))
        val builder = StringBuilder()
        excel.sheetIterator().forEach {sheet ->
            sheet.rowIterator().forEach { row ->
                row.cellIterator().forEach {cell ->
                    when (cell.cellType) {
                        CellType.STRING -> builder.append(cell.stringCellValue)
                        CellType.NUMERIC -> builder.append(cell.numericCellValue)
                        else -> {}
                    }
                    builder.append(" ")
                }
            }
        }
        builder.toString()
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readXls(path: String) : String = try {
        val excel = HSSFWorkbook(FileInputStream(path))
        val builder = StringBuilder()
        excel.sheetIterator().forEach { sheet ->
            sheet.forEach { row ->
                row.forEach { cell ->
                    when (cell.cellType) {
                        CellType.STRING -> builder.append(cell.stringCellValue)
                        CellType.NUMERIC -> builder.append(cell.numericCellValue)
                        else -> {}
                    }
                }
            }
        }
        builder.toString()
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }

    private fun readPublisher(path: String) : String = try {
        PublisherTextExtractor(FileInputStream(path)).text
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }


    private fun readVisio(path: String) : String = try {
        VisioTextExtractor(FileInputStream(path)).text
    } catch (e: Exception) {
        e.message ?: "[Error]"
    }


    private fun readNormal(path: String) : String {
        val file = File(path)
        val builder = StringBuilder()
        file.forEachLine { builder.append(it) }
        return builder.toString()
    }
}
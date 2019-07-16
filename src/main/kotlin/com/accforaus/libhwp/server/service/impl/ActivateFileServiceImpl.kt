package com.accforaus.libhwp.server.service.impl

import com.accforaus.libhwp.server.service.ActiveFileService
import com.tang.hwplib.objects.HWPDocument
import com.tang.hwplib.objects.bodytext.control.HWPControl
import com.tang.hwplib.objects.bodytext.control.HWPControlTable
import com.tang.hwplib.objects.bodytext.control.gso.HWPControlPicture
import org.springframework.stereotype.Service

@Service
class ActivateFileServiceImpl : ActiveFileService {
    private val activeFileMap = HashMap<Int, HWPDocument>()

    override fun addActiveFile(hwp: HWPDocument) : HashMap<String, String> {
        val nextIndex = activeFileMap.size
        val fileInfo = hashMapOf<String, String>(
                "name" to hwp.toFile().name,
                "size" to (hwp.toFile().length() / (1024 * 1024)).toString(),
                "version" to hwp.fileHeader.version.toString(),
                "paragraph" to hwp.bodyText.sectionList[0].paragraphList.size.toString(),
                "table" to getControlLengthByType(hwp, FindType.Table).toString(),
                "picture" to getControlLengthByType(hwp, FindType.Picture).toString(),
                "control" to getControlLengthByType(hwp, FindType.Control).toString(),
                "index" to nextIndex.toString()
        )
        activeFileMap[nextIndex] = hwp

        return fileInfo
    }

    override fun getActiveFileByName(index: Int): HWPDocument? {
        if (activeFileMap.containsKey(index))
            return activeFileMap[index]
        return null
    }

    override fun deactivateFileByName(index: Int) {
        if (activeFileMap.containsKey(index)) activeFileMap.remove(index)
    }

    override fun deactivateAllFiles() {
        for (key in activeFileMap.keys) {
            activeFileMap.remove(key)
        }
    }

    private fun getControlLengthByType(hwp: HWPDocument, type: FindType) : Int {
        var length = 0
        for (section in hwp.bodyText.sectionList) {
            for (paragraph in section.paragraphList) {
                paragraph.controlList?.run {
                    for (control in this) {
                        when (type) {
                            FindType.Control -> length += 1
                            FindType.Table -> if (control is HWPControlTable) length += 1
                            FindType.Picture -> if (control is HWPControlPicture) length += 1
                        }
                    }
                }
            }
        }
        return length
    }

    private enum class FindType {
        Control, Table, Picture
    }
}
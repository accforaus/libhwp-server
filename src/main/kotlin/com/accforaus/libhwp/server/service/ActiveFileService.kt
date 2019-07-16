package com.accforaus.libhwp.server.service

import com.tang.hwplib.objects.HWPDocument

interface ActiveFileService {
    fun addActiveFile(hwp: HWPDocument) : HashMap<String, String>
    fun getActiveFileByName(index: Int) : HWPDocument?
    fun deactivateFileByName(index: Int)
    fun deactivateAllFiles()
}
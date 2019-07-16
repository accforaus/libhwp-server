package com.accforaus.libhwp.server

import com.tang.hwplib.objects.HWPDocument
import org.junit.Test

class HWPLibraryTests {
    companion object {
        const val HWP_PATH = "/Users/josh/ChinaProject/hwp/capstone/"
    }

    @Test
    fun `라이브러리 로드 테스트`() {
        val hwp = HWPDocument("/Users/josh/ChinaProject/hwp/capstone/2019학년도1학기휴복학신청안내(2차).hwp")

        hwp.write("/Users/josh/ChinaProject/hwp/capstone/2019학년도1학기휴복학신청안내(2차) - write.hwp")
    }
}
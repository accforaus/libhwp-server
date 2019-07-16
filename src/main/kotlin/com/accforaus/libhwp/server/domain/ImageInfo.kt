package com.accforaus.libhwp.server.domain

import java.io.Serializable

class ImageInfo : Serializable {
    lateinit var name: String
    var size: Int = 0
    lateinit var path: String
}
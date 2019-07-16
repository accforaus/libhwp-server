package com.accforaus.libhwp.server.domain

class FileInfo {
    lateinit var filename: String
    lateinit var directory: String
    lateinit var content: String
    lateinit var extension: String
    var size: Int = 0
}
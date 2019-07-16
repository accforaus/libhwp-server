package com.accforaus.libhwp.server.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "file")
class FileDirectoryProperty {
    val dir = HashMap<String, String>()
}
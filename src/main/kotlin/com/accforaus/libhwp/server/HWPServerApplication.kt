package com.accforaus.libhwp.server

import com.google.gson.Gson
import com.accforaus.libhwp.server.property.FileDirectoryProperty
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(FileDirectoryProperty::class)
class HWPServerApplication {

	@Bean
	fun getGsonObject() : Gson {
		return Gson()
	}
}

fun main(args: Array<String>) {
	runApplication<HWPServerApplication>(*args)
}

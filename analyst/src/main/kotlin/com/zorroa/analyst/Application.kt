package com.zorroa.analyst

import com.zorroa.common.util.Json
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    Json.configureObjectMapper(Json.Mapper)
    SpringApplication.run(Application::class.java, *args)
}


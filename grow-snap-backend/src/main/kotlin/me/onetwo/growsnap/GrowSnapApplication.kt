package me.onetwo.growsnap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GrowSnapBackendApplication

fun main(args: Array<String>) {
    runApplication<GrowSnapBackendApplication>(*args)
}

package com.usktea.lunch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LunchApplication

fun main(args: Array<String>) {
    runApplication<LunchApplication>(*args)
}

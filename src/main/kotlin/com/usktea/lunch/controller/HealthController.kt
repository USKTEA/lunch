package com.usktea.lunch.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/check")
    fun check(): String {
        return "ok"
    }
}

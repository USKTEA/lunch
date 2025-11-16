package com.usktea.lunch.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebPageController {
    @GetMapping("/web/main")
    fun lunchPage(): String {
        return "forward:/index.html"
    }
}

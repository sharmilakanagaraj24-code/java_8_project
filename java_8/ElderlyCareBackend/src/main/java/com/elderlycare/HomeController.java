
package com.elderlycare;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Elderly Care Backend is running successfully!";
    }

    @GetMapping("/api/test")
    public String test() {
        return "API is working!";
    }
}

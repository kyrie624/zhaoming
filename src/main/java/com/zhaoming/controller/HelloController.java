package com.zhaoming.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 骨架自检接口，正式开发时可删除
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "hello zhaoming";
    }
}

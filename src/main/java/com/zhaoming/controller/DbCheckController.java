package com.zhaoming.controller;

import com.zhaoming.mapper.DbCheckMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库连接自检接口，正式开发时可删除
 */
@RestController
@RequestMapping("/db")
public class DbCheckController {

    private final DbCheckMapper dbCheckMapper;

    public DbCheckController(DbCheckMapper dbCheckMapper) {
        this.dbCheckMapper = dbCheckMapper;
    }

    @GetMapping("/check")
    public String check() {
        dbCheckMapper.ping();
        return "db ok";
    }
}

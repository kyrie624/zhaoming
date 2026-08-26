package com.zhaoming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZhaomingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhaomingApplication.class, args);
    }
}

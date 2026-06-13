package com.nekocafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NekoCafeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoCafeApplication.class, args);
    }
}

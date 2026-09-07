package com.example.aispringboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value={"com.example.aispringboot.mapper"})
@SpringBootApplication
public class AiSpringbootApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiSpringbootApplication.class, (String[])args);
    }
}


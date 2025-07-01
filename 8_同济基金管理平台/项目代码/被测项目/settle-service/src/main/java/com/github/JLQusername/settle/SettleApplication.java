package com.github.JLQusername.settle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.github.JLQusername.api.client")
@MapperScan("com.github.JLQusername.settle.mapper")
@SpringBootApplication
public class SettleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettleApplication.class, args);
    }
}
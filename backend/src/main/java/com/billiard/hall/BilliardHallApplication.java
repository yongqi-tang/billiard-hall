package com.billiard.hall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自主台球厅系统 后端启动类
 */
@SpringBootApplication
@MapperScan("com.billiard.hall.mapper")
@EnableScheduling
public class BilliardHallApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilliardHallApplication.class, args);
    }
}

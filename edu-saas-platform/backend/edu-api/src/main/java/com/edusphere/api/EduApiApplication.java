package com.edusphere.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.edusphere.**.mapper")
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.edusphere")
public class EduApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduApiApplication.class, args);
    }
}

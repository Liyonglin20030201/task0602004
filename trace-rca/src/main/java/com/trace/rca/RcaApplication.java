package com.trace.rca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RcaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RcaApplication.class, args);
    }
}

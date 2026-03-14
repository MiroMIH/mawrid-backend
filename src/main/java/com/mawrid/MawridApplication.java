package com.mawrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableCaching
public class MawridApplication {

    public static void main(String[] args) {
        SpringApplication.run(MawridApplication.class, args);
    }
}

package com.startup.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SecureAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureAuthApplication.class, args);
    }
}

package com.wildlife.programme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
public class ProgrammeApplication {

    public static void main(String[] args) {
        // "Today" and "current month" are evaluated in APP_TIMEZONE (CONTRACTS.md 3.1)
        String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(ProgrammeApplication.class, args);
    }
}

package com.wildlife.patrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** A Clock bean so "today" and "current month" logic can be tested with a fixed clock. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // The JVM default zone is set from APP_TIMEZONE in the Application class before Spring starts.
        return Clock.systemDefaultZone();
    }
}

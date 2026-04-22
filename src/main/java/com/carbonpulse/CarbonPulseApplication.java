package com.carbonpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CarbonPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarbonPulseApplication.class, args);
    }

}

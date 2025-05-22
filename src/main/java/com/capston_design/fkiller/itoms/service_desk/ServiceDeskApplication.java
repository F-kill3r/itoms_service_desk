package com.capston_design.fkiller.itoms.service_desk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class ServiceDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDeskApplication.class, args);
    }
}

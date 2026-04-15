package com.fellowpick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring Boot entry point for the Fellowpick application.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class FellowpickApplication {

	// Bootstraps the Spring application context and starts the embedded server.
	public static void main(String[] args) {
		SpringApplication.run(FellowpickApplication.class, args);
	}

}

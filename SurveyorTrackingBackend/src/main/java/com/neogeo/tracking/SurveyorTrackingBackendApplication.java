package com.neogeo.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.neogeo.tracking")
public class SurveyorTrackingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurveyorTrackingBackendApplication.class, args);
	}

}

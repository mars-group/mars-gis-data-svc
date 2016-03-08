package org.mars_group;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class GisImportApplication {

	public static void main(String[] args) {
		SpringApplication.run(GisImportApplication.class, args);
	}
}

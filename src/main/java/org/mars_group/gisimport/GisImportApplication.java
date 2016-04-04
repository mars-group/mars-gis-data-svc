package org.mars_group.gisimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import java.io.IOException;

@SpringBootApplication
@EnableEurekaClient
public class GisImportApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(GisImportApplication.class, args);
    }
}

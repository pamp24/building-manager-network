package com.BuildingManager.building_manager_network;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.bind.annotation.ExceptionHandler;

@SpringBootApplication
@EnableJpaAuditing
public class BuildingManagerNetworkApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuildingManagerNetworkApiApplication.class, args);
	}

}

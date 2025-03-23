package com.BuildingManager.building_manager_network;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class BuildingManagerNetworkApiApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(BuildingManagerNetworkApiApplication.class, args);
	}

}

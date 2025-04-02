package com.BuildingManager.building_manager_network;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@ComponentScan(basePackages = "com.BuildingManager.building_manager_network") // ✅ Ensure all beans are found
@EnableJpaRepositories(basePackages = "com.BuildingManager.building_manager_network")
public class BuildingManagerNetworkApiApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {

		SpringApplication.run(BuildingManagerNetworkApiApplication.class, args);
	}

}

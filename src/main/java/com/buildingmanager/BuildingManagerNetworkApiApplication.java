package com.buildingmanager;

import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;


@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableJpaRepositories(basePackages = "com.buildingmanager")
public class BuildingManagerNetworkApiApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {

		SpringApplication.run(BuildingManagerNetworkApiApplication.class, args);
	}
	@Bean
	public CommandLineRunner runner(RoleRepository roleRepository) {
		return args -> {
			List<String> roles = List.of("User");
			roles.forEach(roleName -> {
				if (roleRepository.findByName(roleName).isEmpty()) {
					roleRepository.save(Role.builder().name(roleName).build());
				}
			});
		};
	}
}

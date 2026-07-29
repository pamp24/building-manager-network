package com.buildingmanager.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RoleSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> roles = List.of(
                "Admin",
                "User",
                "Resident",
                "Owner",
                "PropertyManager",
                "BuildingManager",
                "PropertyAgent",
                "AdminAgent"
        );

        for (String roleName : roles) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Added role: {}", roleName);
            }
        }
    }
}

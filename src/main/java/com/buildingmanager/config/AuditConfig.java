package com.buildingmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<Integer> auditorProvider() {
        return new ApplicationAuditAware();
    }
}


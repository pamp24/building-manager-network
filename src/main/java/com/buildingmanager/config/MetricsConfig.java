package com.buildingmanager.config;

import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.payment.PaymentRepository;
import com.buildingmanager.user.UserRepository;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterBinder userMetrics(UserRepository userRepository) {
        return registry -> registry.gauge("buildingmanager.users.total", userRepository, r -> (double) r.count());
    }

    @Bean
    public MeterBinder buildingMetrics(BuildingRepository buildingRepository) {
        return registry -> registry.gauge("buildingmanager.buildings.total", buildingRepository, r -> (double) r.count());
    }

    @Bean
    public MeterBinder paymentMetrics(PaymentRepository paymentRepository) {
        return registry -> registry.gauge("buildingmanager.payments.total", paymentRepository, r -> (double) r.count());
    }
}

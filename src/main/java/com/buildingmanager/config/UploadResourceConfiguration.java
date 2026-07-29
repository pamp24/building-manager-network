package com.buildingmanager.config;

import com.buildingmanager.fileStorage.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class UploadResourceConfiguration
        implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        String rootLocation =
                Paths.get(
                                properties.getRootDirectory()
                        )
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();

        String prefix = properties.getPublicPrefix();

        registry
                .addResourceHandler(prefix + "/**")
                .addResourceLocations(rootLocation);

        registry
                .addResourceHandler("/api/v1" + prefix + "/**")
                .addResourceLocations(rootLocation);
    }
}
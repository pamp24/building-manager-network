package com.buildingmanager.fileStorage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.uploads")
public class FileStorageProperties {

    private String rootDirectory = "uploads";

    private String publicPrefix = "/uploads";

    private long maxFileSizeBytes = 10L * 1024L * 1024L;
}
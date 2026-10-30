package com.buildingmanager.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditMessageRequest {
    @NotNull
    @Size(max = 4000)
    private String content;
}

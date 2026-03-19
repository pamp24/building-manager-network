package com.buildingmanager.supportTicket;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCommentRequest {

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Comment type is required")
    private TicketCommentType type;
}

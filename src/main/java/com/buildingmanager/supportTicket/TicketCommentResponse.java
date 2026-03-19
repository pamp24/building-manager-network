package com.buildingmanager.supportTicket;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TicketCommentResponse {

    private Integer id;
    private String message;
    private TicketCommentType type;
    private Integer createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
}
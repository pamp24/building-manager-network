package com.buildingmanager.supportTicket.dto;

import com.buildingmanager.supportTicket.SupportTicketCategory;
import com.buildingmanager.supportTicket.SupportTicketPriority;
import com.buildingmanager.supportTicket.SupportTicketStatus;
import com.buildingmanager.supportTicket.SupportTicketTargetRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupportTicketResponse {

    private Integer id;
    private String ticketNumber;
    private String title;
    private String description;
    private SupportTicketStatus status;
    private SupportTicketPriority priority;
    private SupportTicketCategory category;

    private Integer buildingId;
    private String buildingName;

    private Integer apartmentId;
    private String apartmentLabel;

    private Integer createdByUserId;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    private Integer assignedAgentId;
    private String assignedAgentName;


    private SupportTicketTargetRole targetRole;
}
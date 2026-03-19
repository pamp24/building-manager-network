package com.buildingmanager.supportTicket.dto;

import com.buildingmanager.supportTicket.SupportTicketCategory;
import com.buildingmanager.supportTicket.SupportTicketPriority;
import com.buildingmanager.supportTicket.SupportTicketTargetRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketRequest {

    @NotNull(message = "Building id is required")
    private Integer buildingId;

    private Integer apartmentId;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private SupportTicketPriority priority;

    @NotNull(message = "Category is required")
    private SupportTicketCategory category;

    @NotNull(message = "Target role is required")
    private SupportTicketTargetRole targetRole;
}
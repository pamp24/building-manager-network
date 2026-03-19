package com.buildingmanager.supportTicket;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketStatusRequest {
    private SupportTicketStatus status;
}
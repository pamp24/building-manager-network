package com.buildingmanager.supportTicket;

import com.buildingmanager.supportTicket.dto.SupportTicketRequest;
import com.buildingmanager.supportTicket.dto.SupportTicketResponse;
import com.buildingmanager.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/support-tickets")
@CrossOrigin
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ResponseEntity<SupportTicketResponse> createTicket(
            Authentication authentication,
            @Valid @RequestBody SupportTicketRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.createTicket(user, request));
    }

    @GetMapping("/myTickets")
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getMyTickets(user));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<SupportTicketResponse>> getCompanyTickets(
            Authentication authentication,
            @PathVariable Integer companyId
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getCompanyTickets(user, companyId));
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<SupportTicketResponse>> getInboxTickets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getInboxTickets(user));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<SupportTicketResponse> getTicketById(
            Authentication authentication,
            @PathVariable Integer ticketId
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getTicketById(user, ticketId));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            Authentication authentication,
            @PathVariable Integer ticketId,
            @RequestBody UpdateTicketStatusRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.updateStatus(user, ticketId, request.getStatus()));
    }

    @PatchMapping("/{ticketId}/assign-agent")
    public ResponseEntity<SupportTicketResponse> assignAgent(
            Authentication authentication,
            @PathVariable Integer ticketId,
            @RequestBody AssignAgentRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.assignAgent(user, ticketId, request.getAgentId()));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            Authentication authentication,
            @PathVariable Integer ticketId
    ) {
        User user = (User) authentication.getPrincipal();
        supportTicketService.deleteTicket(user, ticketId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            Authentication authentication,
            @PathVariable Integer ticketId,
            @Valid @RequestBody TicketCommentRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.addComment(user, ticketId, request));
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<TicketCommentResponse>> getComments(
            Authentication authentication,
            @PathVariable Integer ticketId
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getComments(user, ticketId));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<TicketAgentResponse>> getAgents(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getAvailableAgents(user));
    }

    @GetMapping("/list-view")
    public ResponseEntity<List<SupportTicketResponse>> getListViewTickets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(supportTicketService.getListViewTickets(user));
    }
}
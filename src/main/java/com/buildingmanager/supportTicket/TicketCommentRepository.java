package com.buildingmanager.supportTicket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Integer> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);

    void deleteByTicketId(Integer ticketId);
}
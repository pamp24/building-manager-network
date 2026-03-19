package com.buildingmanager.supportTicket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {

    List<SupportTicket> findByCreatedByIdOrderByCreatedAtDesc(Integer createdById);

    List<SupportTicket> findByCompanyIdOrderByCreatedAtDesc(Integer companyId);

    @Query(value = "SELECT nextval('support_ticket_number_seq')", nativeQuery = true)
    Long getNextTicketSequenceValue();


    List<SupportTicket> findByTargetRoleOrderByCreatedAtDesc(SupportTicketTargetRole targetRole);
    List<SupportTicket> findByAssignedAgentIdOrderByCreatedAtDesc(Integer assignedAgentId);

    @Query("""
    SELECT t
    FROM SupportTicket t
    WHERE t.createdBy.id = :userId
       OR t.targetRole = :targetRole
    ORDER BY t.createdAt DESC
""")
    List<SupportTicket> findVisibleTicketsForUser(Integer userId, SupportTicketTargetRole targetRole);

    @Query("""
    SELECT t
    FROM SupportTicket t
    WHERE t.createdBy.id = :userId
       OR t.assignedAgent.id = :userId
    ORDER BY t.createdAt DESC
""")
    List<SupportTicket> findVisibleTicketsForAgent(Integer userId);
}
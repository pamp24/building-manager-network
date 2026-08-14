package com.buildingmanager.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    Optional<Conversation> findByTypeAndBuilding_Id(ConversationType type, Integer buildingId);

    @Query("""
            SELECT c FROM Conversation c
            JOIN c.participants p1
            JOIN c.participants p2
            WHERE c.type = com.buildingmanager.chat.ConversationType.PRIVATE
              AND p1.user.id = :user1
              AND p2.user.id = :user2
            """)
    Optional<Conversation> findPrivateBetween(
            @Param("user1") Integer user1,
            @Param("user2") Integer user2
    );
}

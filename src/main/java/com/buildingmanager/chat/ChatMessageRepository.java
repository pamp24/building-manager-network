package com.buildingmanager.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(Integer conversationId);

    Optional<ChatMessage> findFirstByConversation_IdOrderByCreatedAtDesc(Integer conversationId);
}

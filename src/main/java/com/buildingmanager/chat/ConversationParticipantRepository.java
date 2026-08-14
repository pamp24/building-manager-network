package com.buildingmanager.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Integer> {

    List<ConversationParticipant> findByUser_Id(Integer userId);

    List<ConversationParticipant> findByConversation_Id(Integer conversationId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(Integer conversationId, Integer userId);

    void deleteByConversation_Id(Integer conversationId);
}

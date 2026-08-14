package com.buildingmanager.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    private Integer id;
    private String type;
    private Integer buildingId;
    private String name;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private List<ParticipantDTO> members;
}

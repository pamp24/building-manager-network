package com.buildingmanager.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private boolean mine;
}

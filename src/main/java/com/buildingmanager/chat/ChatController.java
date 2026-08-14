package com.buildingmanager.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getConversations(Authentication auth) {
        return ResponseEntity.ok(chatService.listConversations(auth));
    }

    @PostMapping("/building/{buildingId}")
    public ResponseEntity<ConversationDTO> getOrCreateBuildingConversation(
            @PathVariable Integer buildingId,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.getOrCreateBuildingConversation(buildingId, auth));
    }

    @PostMapping("/private")
    public ResponseEntity<ConversationDTO> getOrCreatePrivateConversation(
            @Valid @RequestBody PrivateChatRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.getOrCreatePrivateConversation(request.getUserId(), auth));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Integer conversationId,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.listMessages(conversationId, auth));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Integer conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, request, auth));
    }

    @PostMapping("/{conversationId}/messages/image")
    public ResponseEntity<ChatMessageDTO> sendImageMessage(
            @PathVariable Integer conversationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            Authentication auth
    ) {
        return ResponseEntity.ok(chatService.sendImageMessage(conversationId, caption, file, auth));
    }
}

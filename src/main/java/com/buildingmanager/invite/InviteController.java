package com.buildingmanager.invite;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping
    public ResponseEntity<InviteResponseDTO> createInvite(
            @RequestBody InviteRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        Invite invite = inviteService.createInvite(
                request.getEmail(),
                request.getRole(),
                request.getApartmentId(),
                currentUser
        );

        return ResponseEntity.ok(inviteService.toDTO(invite));
    }

    @PostMapping("/accept")
    public ResponseEntity<InviteResponseDTO> acceptInvite(
            @RequestParam String code,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Invite invite = inviteService.acceptInvite(code, email);
        return ResponseEntity.ok(inviteService.toDTO(invite));
    }
}
package com.buildingmanager.invite;

import com.buildingmanager.user.User;
import com.buildingmanager.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;
    private final UserService userService; // για currentUser

    @PostMapping("/send")
    public ResponseEntity<InviteResponseDTO> sendInvite(
            @RequestParam String email,
            @RequestParam String role,
            @RequestParam Integer apartmentId,
            Authentication authentication
    ) {
        User manager = (User) authentication.getPrincipal();
        Invite invite = inviteService.createInvite(email, role, apartmentId, manager);
        return ResponseEntity.ok(inviteService.toDTO(invite));
    }


    @PostMapping("/accept")
    public ResponseEntity<InviteResponseDTO> acceptInvite(
            @RequestParam String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        Invite invite = inviteService.acceptInvite(code, user);
        return ResponseEntity.ok(inviteService.toDTO(invite));
    }


}

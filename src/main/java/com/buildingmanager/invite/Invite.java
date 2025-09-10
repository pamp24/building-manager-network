package com.buildingmanager.invite;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String role; // Owner / Resident / BuildingManager

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private String inviteCode; // unique token

    @Enumerated(EnumType.STRING)
    private InviteStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "invited_by")
    private User invitedBy; // ο διαχειριστής που το έστειλε
}

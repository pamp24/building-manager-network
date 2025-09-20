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
    private Integer id;

    private String email;

    private String role; // Owner | Resident

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @ManyToOne
    @JoinColumn(name = "inviter_id")
    private User inviter;
    @Enumerated(EnumType.STRING)
    private InviteStatus status; // Invited | Joined | Expired

    private String token; // για activation link

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7); // 7 μέρες ισχύ
        this.token = UUID.randomUUID().toString();
        this.status = InviteStatus.PENDING;
    }
}

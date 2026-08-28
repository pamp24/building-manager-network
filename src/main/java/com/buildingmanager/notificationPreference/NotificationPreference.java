package com.buildingmanager.notificationPreference;

import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Boolean emailForStatementIssued = true;
    private Boolean emailForNewPoll = false;
    private Boolean emailForNewAnnouncement = false;
    private Boolean emailForAddedToBuilding = true;

    private Boolean appForStatementIssued = true;
    private Boolean appForNewPoll = true;
    private Boolean appForNewAnnouncement = true;
    private Boolean appForAddedToBuilding = true;
    private Boolean appForJoinRequest = true;
    private Boolean appForMemberLeave = true;
    private Boolean appForPaymentCompleted = false;

    private Boolean smsForStatementIssued = false;
    private Boolean smsForNewPoll = false;
    private Boolean smsForNewAnnouncement = false;
    private Boolean smsForAddedToBuilding = false;
}

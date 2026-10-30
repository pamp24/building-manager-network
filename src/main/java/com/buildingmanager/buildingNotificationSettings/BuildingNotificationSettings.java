package com.buildingmanager.buildingNotificationSettings;

import com.buildingmanager.building.Building;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "building_notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingNotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false, unique = true)
    private Building building;

    @Builder.Default
    private Boolean emailForStatementIssued = true;
    @Builder.Default
    private Boolean emailForNewPoll = false;
    @Builder.Default
    private Boolean emailForNewAnnouncement = false;
    @Builder.Default
    private Boolean emailForAddedToBuilding = true;

    @Builder.Default
    private Boolean appForStatementIssued = true;
    @Builder.Default
    private Boolean appForNewPoll = true;
    @Builder.Default
    private Boolean appForNewAnnouncement = true;
    @Builder.Default
    private Boolean appForAddedToBuilding = true;
    @Builder.Default
    private Boolean appForJoinRequest = true;
    @Builder.Default
    private Boolean appForMemberLeave = true;
    @Builder.Default
    private Boolean appForPaymentCompleted = false;

    @Builder.Default
    private Boolean smsForStatementIssued = false;
    @Builder.Default
    private Boolean smsForNewPoll = false;
    @Builder.Default
    private Boolean smsForNewAnnouncement = false;
    @Builder.Default
    private Boolean smsForAddedToBuilding = false;

    @Builder.Default
    private Boolean managerAppForApartmentChanges = true;
    @Builder.Default
    private Boolean managerEmailForApartmentChanges = true;
    @Builder.Default
    private Boolean managerAppForMemberLeave = true;
    @Builder.Default
    private Boolean managerEmailForMemberLeave = true;
    @Builder.Default
    private Boolean managerAppForAddedToBuilding = true;
    @Builder.Default
    private Boolean managerEmailForAddedToBuilding = true;

    @Builder.Default
    private Boolean membersCanCreateAnnouncement = false;
    @Builder.Default
    private Boolean membersCanCreatePoll = false;
}

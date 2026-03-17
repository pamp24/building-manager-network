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

    private Boolean emailForStatementIssued = true;
    private Boolean emailForNewPoll = false;
    private Boolean emailForNewAnnouncement = false;

    private Boolean appForJoinRequest = true;
    private Boolean appForMemberLeave = true;
    private Boolean appForPaymentCompleted = false;
    private Boolean appForNewPoll = false;
    private Boolean appForNewAnnouncement = false;

    private Boolean managerEmailForApartmentChanges = true;
    private Boolean managerEmailForDirectMessage = false;
    private Boolean managerEmailForAddedToBuilding = true;
}
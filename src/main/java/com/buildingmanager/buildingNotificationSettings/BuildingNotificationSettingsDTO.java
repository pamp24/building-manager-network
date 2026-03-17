package com.buildingmanager.buildingNotificationSettings;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingNotificationSettingsDTO {

    private Integer buildingId;

    private Boolean emailForStatementIssued;
    private Boolean emailForNewPoll;
    private Boolean emailForNewAnnouncement;

    private Boolean appForJoinRequest;
    private Boolean appForMemberLeave;
    private Boolean appForPaymentCompleted;
    private Boolean appForNewPoll;
    private Boolean appForNewAnnouncement;

    private Boolean managerEmailForApartmentChanges;
    private Boolean managerEmailForDirectMessage;
    private Boolean managerEmailForAddedToBuilding;
}

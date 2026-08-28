package com.buildingmanager.notificationPreference;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDTO {

    private Integer userId;

    private Boolean emailForStatementIssued;
    private Boolean emailForNewPoll;
    private Boolean emailForNewAnnouncement;
    private Boolean emailForAddedToBuilding;

    private Boolean appForStatementIssued;
    private Boolean appForNewPoll;
    private Boolean appForNewAnnouncement;
    private Boolean appForAddedToBuilding;
    private Boolean appForJoinRequest;
    private Boolean appForMemberLeave;
    private Boolean appForPaymentCompleted;

    private Boolean smsForStatementIssued;
    private Boolean smsForNewPoll;
    private Boolean smsForNewAnnouncement;
    private Boolean smsForAddedToBuilding;
}

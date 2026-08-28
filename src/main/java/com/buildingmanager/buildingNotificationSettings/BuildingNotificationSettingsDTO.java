package com.buildingmanager.buildingNotificationSettings;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingNotificationSettingsDTO {

    private Integer buildingId;

    private Boolean managerAppForApartmentChanges;
    private Boolean managerEmailForApartmentChanges;
    private Boolean managerAppForMemberLeave;
    private Boolean managerEmailForMemberLeave;
    private Boolean managerAppForAddedToBuilding;
    private Boolean managerEmailForAddedToBuilding;

    private Boolean membersCanCreateAnnouncement;
    private Boolean membersCanCreatePoll;
}

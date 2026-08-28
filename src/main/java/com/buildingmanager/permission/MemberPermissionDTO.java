package com.buildingmanager.permission;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPermissionDTO {

    private Integer userId;
    private String fullName;
    private String email;
    private String role;
    private Boolean isManager;
    private Boolean canCreateAnnouncement;
    private Boolean canCreatePoll;
}

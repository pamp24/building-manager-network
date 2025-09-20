package com.buildingmanager.invite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteAcceptRequest {
    private String code;
    private String email;
    private Integer apartmentId;
    private String role;
}

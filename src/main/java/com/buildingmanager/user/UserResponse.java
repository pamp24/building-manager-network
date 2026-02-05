package com.buildingmanager.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String firstName;
    private String lastName;
    private Integer id;
    private String email;
    private String password;
    private String name;
    private String role;
    private String profileImageUrl;

}


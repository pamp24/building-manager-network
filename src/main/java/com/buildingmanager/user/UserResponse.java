package com.buildingmanager.user;

import com.buildingmanager.role.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
}
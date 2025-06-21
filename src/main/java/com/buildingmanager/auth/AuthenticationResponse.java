package com.buildingmanager.auth;


import com.buildingmanager.user.UserResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {

    private String token;
    private UserResponse user;

}

package com.BuildingManager.building_manager_network.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegistrationRequest {

    @NotEmpty(message = "First Name is Mandatory")
    @NotBlank(message = "First Name is Mandatory")
    private String firstName;
    @NotEmpty(message = "Last Name is Mandatory")
    @NotBlank(message = "Last Name is Mandatory")
    private String lastName;
    @Email(message = "Email is not formated")
    @NotEmpty(message = "Email is Mandatory")
    @NotBlank(message = "Email is Mandatory")
    private String email;
    @NotEmpty(message = "Password is Mandatory")
    @NotBlank(message = "Password is Mandatory")
    @Size(min = 8, message = "Password should be 8 characters long minimum" )
    private String password;
}

package com.buildingmanager.auth;


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

    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    private String firstName;

    @NotBlank(message = "Το επώνυμο είναι υποχρεωτικό")
    private String lastName;

    @NotBlank(message = "Το email είναι υποχρεωτικό")
    @Email(message = "Το email δεν είναι σωστά διαμορφωμένο")
    private String email;

    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός")
    @Size(min = 8, message = "Ο κωδικός πρέπει να περιέχει τουλάχιστον 8 χαρακτήρες")
    private String password;

}

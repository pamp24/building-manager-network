package com.buildingmanager.user;

import com.buildingmanager.role.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class UserDTO {

    private Integer id;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;

    private String address1;
    private String addressNumber1;
    private String address2;
    private String addressNumber2;
    private String country;
    private String state;
    private String city;
    private String region;
    private String postalCode;

    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime lastLoginDate;

    private boolean enabled;
    private boolean accountLocked;

    private List<String> roles;

    public static UserDTO fromUser(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.fullName())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())

                .address1(user.getAddress1())
                .addressNumber1(user.getAddressNumber1())
                .address2(user.getAddress2())
                .addressNumber2(user.getAddressNumber2())
                .country(user.getCountry())
                .region(user.getRegion())
                .postalCode(user.getPostalCode())

                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .lastLoginDate(user.getLastLoginDate())

                .enabled(user.isEnabled())
                .accountLocked(user.isAccountNonLocked() == false)

                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .build();
    }
}

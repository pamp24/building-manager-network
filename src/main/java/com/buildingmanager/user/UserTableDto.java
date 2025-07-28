package com.buildingmanager.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTableDto {
    private String name;
    private String email;
    private String role;
    private String status;
}



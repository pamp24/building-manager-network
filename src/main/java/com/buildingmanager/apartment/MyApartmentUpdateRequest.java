package com.buildingmanager.apartment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyApartmentUpdateRequest {

    private Integer id;

    private String ownerFirstName;
    private String ownerLastName;

    private String residentFirstName;
    private String residentLastName;

    private String description;
}

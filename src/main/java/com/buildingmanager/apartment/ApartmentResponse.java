package com.buildingmanager.apartment;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentResponse {


    private String number;
    private String sqMetersApart;
    private int floor;
    private boolean parkingSpace;
    private boolean active;
    private boolean enable;
    private String buildingName;
    private String fullApartmentName;

}

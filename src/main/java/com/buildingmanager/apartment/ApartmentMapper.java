package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import org.springframework.stereotype.Service;


@Service
public class ApartmentMapper {
    public Apartment toApartment(ApartmentRequest request) {
        return Apartment.builder()
                .id(request.buildingId())
                .number(request.number())
                .sqMetersApart(request.sqMetersApart())
                .floor(request.floor())
                .parkingSpace(request.parkingSpace())
                .active(request.active())
                .enable(request.enable())
                .building(Building.builder()
                        .id(request.buildingId())
                        .active(false)
                        .enable(false)
                        .build()
                )
                .build();
    }

    public Object toApartmentResponse(Apartment apartment, int id) {

        return ApartmentResponse.builder()
                .fullApartmentName(apartment.fullApartmentName())
                .number(apartment.getNumber())
                .sqMetersApart(apartment.getSqMetersApart())
                .floor(apartment.getFloor())
                .parkingSpace(apartment.isParkingSpace())
                .active(false)
                .enable(false)
                .build();

    }
}

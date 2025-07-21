package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import com.buildingmanager.user.User;
import org.springframework.stereotype.Service;


@Service
public class ApartmentMapper {
    public Apartment toApartment(ApartmentRequest request) {
        return Apartment.builder()
                .fullName(request.fullName())
                .tenantFullName(request.tenantFullName())
                .number(request.number())
                .sqMetersApart(request.sqMetersApart())
                .floor(request.floor())
                .parkingSpace(request.parkingSpace())
                .isRented(request.isRented())
                .tenantFullName(request.tenantFullName())
                .parkingSlot(request.parkingSlot())
                .commonPercent(request.commonPercent())
                .elevatorPercent(request.elevatorPercent())
                .heatingPercent(request.heatingPercent())
                .active(request.active())
                .enable(request.enable())
                .building(Building.builder()
                        .id(request.buildingId())
                        .build()
                )
                .build();
    }

    public Object toApartmentResponse(Apartment apartment, int id) {
        User manager = apartment.getBuilding().getManager();
        return ApartmentResponse.builder()
                .fullApartmentName(apartment.fullApartmentName())
                .fullName(apartment.getFullName())
                .isRented(apartment.getIsRented())
                .tenantFullName(apartment.getTenantFullName())
                .number(apartment.getNumber())
                .sqMetersApart(String.valueOf(apartment.getSqMetersApart()))
                .floor(apartment.getFloor())
                .parkingSpace(apartment.isParkingSpace())
                .parkingSlot(apartment.getParkingSlot())
                .commonPercent(apartment.getCommonPercent())
                .elevatorPercent(apartment.getElevatorPercent())
                .heatingPercent(apartment.getHeatingPercent())
                .active(false)
                .enable(false)
                .managerFullName(manager.fullName())
                .managerId(String.valueOf(manager.getId()))
                .build();

    }
}

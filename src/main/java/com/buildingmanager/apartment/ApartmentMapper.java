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
                .parkingSlot(request.parkingSlot())
                .commonPercent(request.commonPercent())
                .elevatorPercent(request.elevatorPercent())
                .heatingPercent(request.heatingPercent())
                .apStorageExist(request.apStorageExist())
                .storageSlot(request.storageSlot())
                .isManagerHouse(request.isManagerHouse())
                .apDescription(request.apDescription())
                .active(request.active())
                .enable(request.enable())
                .building(Building.builder().id(request.buildingId()).build())
                .resident(request.residentId() != null ? User.builder().id(request.residentId()).build() : null)
                .owner(request.ownerId() != null ? User.builder().id(request.ownerId()).build() : null)
                .build();
    }


    public ApartmentResponse toApartmentResponse(Apartment apartment, int id) {

        User manager = apartment.getBuilding().getManager();
        User owner = apartment.getOwner();

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
                .apStorageExist(apartment.isApStorageExist())
                .storageSlot(apartment.getStorageSlot())
                .isManagerHouse(apartment.isManagerHouse())
                .apDescription(apartment.getApDescription())
                .active(false)
                .enable(false)
                .managerFullName(manager.fullName())
                .managerId(String.valueOf(manager.getId()))
                .resident(apartment.getResident() != null ? String.valueOf(apartment.getResident().getId()) : null)
                .owner(apartment.getOwner() != null ? String.valueOf(apartment.getOwner().getId()) : null)
                .owner(owner != null ? String.valueOf(owner.getId()) : null)
                .ownerFullName(owner.fullName())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerPhone(owner.getPhoneNumber())
                .ownerStreet(owner.getAddress1())
                .ownerStreetNumber(owner.getAddressNumber1())
                .ownerCity(owner.getCity())

                .build();

    }
}

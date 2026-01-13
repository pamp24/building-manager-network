    package com.buildingmanager.apartment;

    import com.buildingmanager.building.Building;
    import com.buildingmanager.user.User;
    import org.springframework.stereotype.Service;



    @Service
    public class ApartmentMapper {
        public Apartment toApartment(ApartmentRequest request) {
            return Apartment.builder()
                    .ownerFirstName(request.ownerFirstName())
                    .ownerLastName(request.ownerLastName())
                    .number(request.number())
                    .sqMetersApart(request.sqMetersApart())
                    .floor(request.floor())
                    .parkingSpace(request.parkingSpace())
                    .isRented(request.isRented())
                    .residentFirstName(request.residentFirstName())
                    .residentLastName(request.residentLastName())
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


        public ApartmentResponse toApartmentResponse(Apartment apartment, Integer id) {
            if (apartment == null) {
                return null;
            }
            Building building = apartment.getBuilding();
            User manager = (building != null ? building.getManager() : null);
            User owner   = apartment.getOwner();
            User resident = apartment.getResident();
            return ApartmentResponse.builder()
                    .id(apartment.getId())
                    .fullApartmentName(apartment.fullApartmentName())
                    .ownerFirstName(apartment.getOwnerFirstName())
                    .ownerLastName(apartment.getOwnerLastName())
                    .isRented(apartment.getIsRented())
                    .residentFirstName(apartment.getResidentFirstName())
                    .residentLastName(apartment.getResidentLastName())
                    .number(apartment.getNumber())
                    .sqMetersApart(String.valueOf(apartment.getSqMetersApart()))
                    .floor(apartment.getFloor())
                    .parkingSpace(apartment.getParkingSpace() != null ? apartment.getParkingSpace() : false)
                    .parkingSlot(apartment.getParkingSlot())
                    .commonPercent(apartment.getCommonPercent())
                    .elevatorPercent(apartment.getElevatorPercent())
                    .heatingPercent(apartment.getHeatingPercent())
                    .apStorageExist(apartment.getApStorageExist() != null ? apartment.getApStorageExist() : false)
                    .storageSlot(apartment.getStorageSlot())
                    .isManagerHouse(apartment.getIsManagerHouse() != null ? apartment.getIsManagerHouse() : false)
                    .apDescription(apartment.getApDescription())

                    .buildingId(apartment.getBuilding().getId())
                    .buildingName(apartment.getBuilding().getName())
                    .buildingStreet(apartment.getBuilding().getStreet1())
                    .buildingStreetNumber(apartment.getBuilding().getStNumber1()    )
                    .buildingCity(apartment.getBuilding().getCity())

                    .active(false)
                    .enable(false)
                    .lastModifiedDate(apartment.getLastModifiedDate())

                    // Manager (null-safe)
                    .managerId(manager != null ? String.valueOf(manager.getId()) : null)
                    .managerFullName(manager != null ? manager.fullName() : null)

                    // Resident (null-safe)
                    .resident(resident != null ? String.valueOf(resident.getId()) : null)
                    .residentId(resident != null ? resident.getId() : null)
                    .residentFullName(resident != null ? resident.fullName() : null)
                    .residentEmail(resident != null ? resident.getEmail() : null)
                    .residentPhone(resident != null ? resident.getPhoneNumber() : null)

                    // Owner (null-safe)
                    .owner(owner != null ? String.valueOf(owner.getId()) : null)
                    .ownerId(owner != null ? owner.getId(): null)
                    .ownerFullName(owner != null ? owner.fullName() : null)
                    .ownerEmail(owner != null ? owner.getEmail() : null)
                    .ownerPhone(owner != null ? owner.getPhoneNumber() : null)
                    .ownerStreet(owner != null ? owner.getAddress1() : null)
                    .ownerStreetNumber(owner != null ? owner.getAddressNumber1() : null)
                    .ownerCity(owner != null ? owner.getCity() : null)


                    .build();

        }
        public ApartmentResponse toApartmentResponse(Apartment apartment) {
            if (apartment == null) return null;
            return toApartmentResponse(apartment, apartment.getId());
        }
    }

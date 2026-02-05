package com.buildingmanager.building;

import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class BuildingMapper {
    private final UserRepository userRepository;

    public BuildingMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Building toBuilding(BuildingRequest request) {
        User manager = null;
        if (request.managerId() != null) {
            manager = userRepository.findById(request.managerId())
                    .orElseThrow(() -> new IllegalArgumentException("Δεν βρέθηκε χρήστης με ID: " + request.managerId()));
        }

        HeatingType heatingType = null;
        if (request.heatingType() != null) {
            try {
                heatingType = HeatingType.valueOf(request.heatingType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Μη έγκυρος τύπος θέρμανσης: " + request.heatingType());
            }
        }

        return Building.builder()
                .id(request.id())
                .name(request.name())
                .street1(request.street1())
                .stNumber1(request.stNumber1())
                .street2(request.street2())
                .stNumber2(request.stNumber2())
                .city(request.city())
                .region(request.region())
                .postalCode(request.postalCode())
                .country(request.country())
                .state(request.state())
                .floors(Integer.valueOf(request.floors()))
                .apartmentsNum(request.apartmentsNum())
                .sqMetersTotal(request.sqMetersTotal())
                .sqMetersCommonSpaces(request.sqMetersCommonSpaces())
                .parkingExist(request.parkingExist())
                .parkingSpacesNum(request.parkingSpacesNum())
                .buildingDescription(request.buildingDescription())
                .hasCentralHeating(request.hasCentralHeating())
                .heatingType(heatingType)
                .heatingCapacityLitres(request.heatingCapacityLitres())
                .manager(manager)
                .undergroundFloorExist(request.undergroundFloorExist())
                .halfFloorExist(request.halfFloorExist())
                .overTopFloorExist(request.overTopFloorExist())
                .managerHouseExist(request.managerHouseExist())
                .storageExist(request.storageExist())
                .storageNum(request.storageNum())
                .active(request.active())
                .enable(request.enable())
                .build();
    }

    public BuildingDTO toDTO(Building building) {
        ManagerDTO managerDTO = null;
        if (building.getManager() != null) {
            managerDTO = new ManagerDTO(
                    building.getManager().getId(),
                    building.getManager().fullName(),
                    building.getManager().getEmail(),
                    building.getManager().getPhoneNumber(),
                    building.getManager().getAddress1(),
                    building.getManager().getAddressNumber1(),
                    building.getManager().getAddress2(),
                    building.getManager().getAddressNumber2(),
                    building.getManager().getProfileImageUrl()
            );
        }

        return BuildingDTO.builder()
                .id(building.getId())
                .name(building.getName())
                .street1(building.getStreet1())
                .stNumber1(building.getStNumber1())
                .street2(building.getStreet2())
                .stNumber2(building.getStNumber2())
                .city(building.getCity())
                .region(building.getRegion())
                .postalCode(building.getPostalCode())
                .country(building.getCountry())
                .floors(building.getFloors())
                .apartmentsNum(building.getApartmentsNum())
                .sqMetersTotal(building.getSqMetersTotal())
                .sqMetersCommonSpaces(building.getSqMetersCommonSpaces())
                .parkingExist(building.isParkingExist())
                .parkingSpacesNum(building.getParkingSpacesNum())
                .buildingDescription(building.getBuildingDescription())
                .hasCentralHeating(building.isHasCentralHeating())
                .heatingType(building.getHeatingType() != null ? building.getHeatingType().name() : null)
                .heatingCapacityLitres(building.getHeatingCapacityLitres())
                .buildingCode(building.getBuildingCode())
                .undergroundFloorExist(building.isUndergroundFloorExist())
                .halfFloorExist(building.isHalfFloorExist())
                .overTopFloorExist(building.isOverTopFloorExist())
                .managerHouseExist(building.isManagerHouseExist())
                .storageExist(building.isStorageExist())
                .storageNum(building.getStorageNum())
                .manager(managerDTO)
                .build();
    }


    public BuildingResponse toBuildingResponse(Building building) {
        return BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .street1(building.getStreet1())
                .stNumber1(building.getStNumber1())
                .street2(building.getStreet2())
                .stNumber2(building.getStNumber2())
                .city(building.getCity())
                .region(building.getRegion())
                .postalCode(building.getPostalCode())
                .country(building.getCountry())
                .state(building.getState())
                .floors(String.valueOf(building.getFloors()))
                .apartmentsNum(building.getApartmentsNum())
                .sqMetersTotal(building.getSqMetersTotal())
                .sqMetersCommonSpaces(building.getSqMetersCommonSpaces())
                .parkingExist(building.isParkingExist())
                .parkingSpacesNum(building.getParkingSpacesNum())
                .active(building.isActive())
                .enable(building.isEnable())
                .buildingCode(building.getBuildingCode())
                .buildingDescription(building.getBuildingDescription())
                .hasCentralHeating(building.isHasCentralHeating())
                .heatingType(building.getHeatingType() != null ? building.getHeatingType().name() : null)
                .heatingCapacityLitres(building.getHeatingCapacityLitres())
                .undergroundFloorExist(building.isUndergroundFloorExist())
                .halfFloorExist(building.isHalfFloorExist())
                .overTopFloorExist(building.isOverTopFloorExist())
                .managerHouseExist(building.isManagerHouseExist())
                .storageExist(building.isStorageExist())
                .storageNum(building.getStorageNum())
                .managerFullName(
                        building.getManager() != null ? building.getManager().fullName() : null
                )
                .managerEmail(
                        building.getManager() != null ? building.getManager().getEmail() : null
                )
                .managerPhone(
                        building.getManager() != null ? building.getManager().getPhoneNumber() : null
                )
                .managerAddress1(
                        building.getManager() != null ? building.getManager().getAddress1() : null
                )
                .managerCity(
                        building.getManager() != null ? building.getManager().getCity() : null
                )
                .managerProfileImgUrl(building.getManager() != null ? building.getManager().getProfileImageUrl() : null
                )
                .build();
    }
}


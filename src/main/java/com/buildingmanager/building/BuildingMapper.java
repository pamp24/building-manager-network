package com.buildingmanager.building;

import org.springframework.stereotype.Service;

@Service
public class BuildingMapper {
    public Building toBuilding(BuildingRequest request) {
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
                .floors(Integer.valueOf(request.floors()))
                .apartmentsNum(request.apartmentsNum())
                .sqMetersTotal(request.sqMetersTotal())
                .sqMetersCommonSpaces(request.sqMetersCommonSpaces())
                .parkingExists(request.parkingExists())
                .parkingSpacesNum(request.parkingSpacesNum())
                .active(request.active())
                .enable(request.enable())
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
                .floors(String.valueOf(building.getFloors()))
                .apartmentsNum(building.getApartmentsNum())
                .sqMetersTotal(building.getSqMetersTotal())
                .sqMetersCommonSpaces(building.getSqMetersCommonSpaces())
                .parkingExists(building.isParkingExists())
                .parkingSpacesNum(building.getParkingSpacesNum())
                .active(building.isActive())
                .enable(building.isEnable())
                .buildingCode(building.getBuildingCode())
                .buildingDescription(building.getBuildingDescription())
                .managerFullName(
                        building.getManager() != null ? building.getManager().fullName() : null
                )
                .managerEmail(
                        building.getManager() !=null ? building.getManager().getEmail() : null
                )
                .managerPhone(
                        building.getManager() !=null ? building.getManager().getPhoneNumber() : null
                )
                .managerAddress1(
                        building.getManager() !=null ? building.getManager().getAddress1() : null
                )
                .managerCity(
                        building.getManager() !=null ? building.getManager().getCity() : null
                )
                .build();
    }
}

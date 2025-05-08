package com.BuildingManager.building_manager_network.building;

import org.springframework.stereotype.Service;

@Service
public class BuildingMapper {
    public Building toBuilding(BuildingRequest request) {
        return Building.builder()
                .id(request.id())
                .name(request.name())
                .street(request.street())
                .stNumber(request.stNumber())
                .city(request.city())
                .region(request.region())
                .postalCode(request.postalCode())
                .country(request.country())
                .floors(request.floors())
                .apartmentsNum(request.apartmentsNum())
                .sqMetersTotal(request.sqMetersTotal())
                .sqMetersCommonSpaces(request.sqMetersCommonSpaces())
                .parkingExists(false)
                .parkingSpacesNum(request.parkingSpacesNum())
                .active(false)
                .enable(false)
                .build();
    }

    public BuildingResponse toBuildingResponse(Building building) {
        return BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .street(building.getStreet())
                .stNumber(building.getStNumber())
                .city(building.getCity())
                .region(building.getRegion())
                .postalCode(building.getPostalCode())
                .country(building.getCountry())
                .floors(building.getFloors())
                .apartmentsNum(building.getApartmentsNum())
                .sqMetersTotal(building.getSqMetersTotal())
                .sqMetersCommonSpaces(building.getSqMetersCommonSpaces())
                .parkingExists(building.isParkingExists())
                .parkingSpacesNum(building.getParkingSpacesNum())
                .active(building.isActive())
                .enable(building.isEnable())
                .build();
    }
}

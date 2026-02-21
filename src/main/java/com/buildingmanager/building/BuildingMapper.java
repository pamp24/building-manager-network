package com.buildingmanager.building;

import com.buildingmanager.company.CompanyDTO;
import org.springframework.stereotype.Service;

@Service
public class BuildingMapper {

    private CompanyDTO toCompanyDTO(com.buildingmanager.company.Company c) {
        CompanyDTO dto = new CompanyDTO();
        dto.setCompanyId(c.getId());
        dto.setCompanyName(c.getCompanyName());
        dto.setTaxNumber(c.getTaxNumber());
        dto.setManagerName(c.getManagerName());
        dto.setEmail(c.getEmail());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setAddressNumber(c.getAddressNumber());
        dto.setPostalCode(c.getPostalCode());
        dto.setCity(c.getCity());
        dto.setRegion(c.getRegion());
        dto.setCountry(c.getCountry());
        return dto;
    }

    public Building toBuilding(BuildingRequest request) {

        HeatingType heatingType = null;
        if (request.heatingType() != null) {
            heatingType = HeatingType.valueOf(request.heatingType().toUpperCase());
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

        CompanyDTO companyDTO = null;
        if (building.getCompany() != null) {
            var c = building.getCompany();
            companyDTO = new CompanyDTO(
                    c.getId(),
                    c.getCompanyName(),
                    c.getTaxNumber(),
                    c.getManagerName(), // θα χαρτογραφηθεί στο responsiblePerson αν ακολουθήσεις το #1
                    c.getPhone(),       // θα χαρτογραφηθεί στο phoneNumber αν ακολουθήσεις το #1
                    c.getEmail(),
                    c.getAddress(),
                    c.getAddressNumber(),
                    c.getPostalCode(),
                    c.getCity(),
                    c.getRegion(),
                    c.getCountry()
            );
        }

        var m = building.getManager();

        return BuildingDTO.builder()
                .id(building.getId())
                .name(building.getName())
                .street1(building.getStreet1())
                .stNumber1(building.getStNumber1())
                .street2(building.getStreet2())
                .stNumber2(building.getStNumber2())
                .city(building.getCity())
                .state(building.getState())
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

                // manager flat
                .managerFullName(m != null ? m.fullName() : null)
                .managerEmail(m != null ? m.getEmail() : null)
                .managerPhone(m != null ? m.getPhoneNumber() : null)
                .managerAddress1(m != null ? m.getAddress1() : null)
                .managerCity(m != null ? m.getCity() : null)
                .managerProfileImgUrl(m != null ? m.getProfileImageUrl() : null)
                .managerRole(m != null && m.getRole() != null ? m.getRole().getName() : null)

                .company(companyDTO)
                .build();
    }


    public BuildingResponse toBuildingResponse(Building building) {
        CompanyDTO companyDTO = building.getCompany() != null ? toCompanyDTO(building.getCompany()) : null;

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
                .floors(building.getFloors())
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
                .managerRole(
                        building.getManager() != null && building.getManager().getRole() != null
                                ? building.getManager().getRole().getName()
                                : null
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
                .company(companyDTO)
                .build();
    }

}


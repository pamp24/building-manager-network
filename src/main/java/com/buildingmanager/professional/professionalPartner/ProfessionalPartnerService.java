package com.buildingmanager.professional.professionalPartner;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.professional.ProfessionalBusinessDTO;
import com.buildingmanager.professional.ProfessionalBusinessRepository;
import com.buildingmanager.professional.ProfessionalBusinessService;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalPartnerService {

    private final ProfessionalPartnerRepository partnerRepository;
    private final BuildingRepository buildingRepository;
    private final ProfessionalBusinessRepository professionalRepository;
    private final ProfessionalBusinessService professionalBusinessService;
    private final BuildingPermissionService buildingPermissionService;

    public void addPartner(Integer buildingId, Integer professionalId, User user) {
        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχετε δικαίωμα διαχείρισης αυτής της πολυκατοικίας.");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found"));

        if (partnerRepository.existsByBuildingAndProfessional(building, professional)) {
            return;
        }

        partnerRepository.save(
                ProfessionalPartner.builder()
                        .building(building)
                        .professional(professional)
                        .addedBy(user)
                        .build()
        );
    }

    public void removePartner(Integer buildingId, Integer professionalId, User user) {
        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχετε δικαίωμα διαχείρισης αυτής της πολυκατοικίας.");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found"));

        partnerRepository.findByBuildingAndProfessional(building, professional)
                .ifPresent(partnerRepository::delete);
    }

    public List<ProfessionalBusinessDTO> getPartners(Integer buildingId, User user) {
        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχετε πρόσβαση σε αυτή την πολυκατοικία.");
        }

        return partnerRepository.findByBuilding_Id(buildingId)
                .stream()
                .map(ProfessionalPartner::getProfessional)
                .map(this::toDTO)
                .toList();
    }

    private ProfessionalBusinessDTO toDTO(ProfessionalBusiness business) {
        return ProfessionalBusinessDTO.builder()
                .id(business.getId())
                .businessName(business.getBusinessName())
                .ownerFullName(business.getOwnerFullName())
                .category(business.getCategory())
                .description(business.getDescription())
                .phone(business.getPhone())
                .email(business.getEmail())
                .website(business.getWebsite())
                .city(business.getCity())
                .region(business.getRegion())
                .address(business.getAddress())
                .taxNumber(business.getTaxNumber())
                .verified(business.isVerified())
                .active(business.isActive())
                .ratingAverage(business.getRatingAverage())
                .reviewCount(business.getReviewCount())

                .createdByUserId(
                        business.getCreatedByUser() != null
                                ? business.getCreatedByUser().getId()
                                : null
                )
                .workingHours(business.getWorkingHours())

                .build();
    }
}
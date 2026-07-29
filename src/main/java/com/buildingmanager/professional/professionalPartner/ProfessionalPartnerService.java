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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfessionalPartnerService {

    private final ProfessionalPartnerRepository partnerRepository;
    private final BuildingRepository buildingRepository;
    private final ProfessionalBusinessRepository professionalRepository;
    private final BuildingPermissionService buildingPermissionService;
    private final ProfessionalBusinessService professionalBusinessService;

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

        log.debug("getPartners: buildingId={}, userId={}, role={}, canView={}",
                buildingId, user.getId(), user.getRole().getName(),
                buildingPermissionService.canViewBuilding(user, buildingId));

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχετε πρόσβαση σε αυτή την πολυκατοικία.");
        }

        return partnerRepository.findByBuilding_Id(buildingId)
                .stream()
                .map(ProfessionalPartner::getProfessional)
                .map(professionalBusinessService::mapToDTO)
                .toList();
    }

}
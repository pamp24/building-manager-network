package com.buildingmanager.professional.professionalPartner;

import com.buildingmanager.building.Building;
import com.buildingmanager.professional.ProfessionalBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;


public interface ProfessionalPartnerRepository
        extends JpaRepository<ProfessionalPartner, Integer> {
        boolean existsByBuildingAndProfessional(
                Building building,
                ProfessionalBusiness professional
        );

        Optional<ProfessionalPartner> findByBuildingAndProfessional(
                Building building,
                ProfessionalBusiness professional
        );

        List<ProfessionalPartner> findByBuilding(Building building);

        List<ProfessionalPartner> findByBuilding_Id(Integer buildingId);
}


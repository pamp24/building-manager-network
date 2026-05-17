package com.buildingmanager.professional;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessionalBusinessRepository extends JpaRepository<ProfessionalBusiness, Integer> {

    List<ProfessionalBusiness> findByActiveTrueAndVerifiedTrueOrderByCreatedAtDesc();

    List<ProfessionalBusiness> findByActiveTrueAndVerifiedTrueAndCategoryOrderByCreatedAtDesc(
            ProfessionalCategory category
    );

    List<ProfessionalBusiness> findByActiveTrueAndVerifiedTrueAndCityContainingIgnoreCaseOrderByCreatedAtDesc(
            String city
    );

    List<ProfessionalBusiness> findByActiveTrueAndVerifiedTrueAndCategoryAndCityContainingIgnoreCaseOrderByCreatedAtDesc(
            ProfessionalCategory category,
            String city
    );

    List<ProfessionalBusiness> findByCreatedByUser_IdOrderByCreatedAtDesc(Integer userId);

    List<ProfessionalBusiness> findByVerifiedFalseOrderByCreatedAtDesc();

    long countByVerifiedFalseAndActiveFalse();

    long countByVerifiedTrueAndActiveTrue();

    long countByActiveFalse();

    long countByVerifiedTrue();
}
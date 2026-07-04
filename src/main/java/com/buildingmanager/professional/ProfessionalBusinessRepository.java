package com.buildingmanager.professional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProfessionalBusinessRepository
        extends JpaRepository<ProfessionalBusiness, Integer>,
        JpaSpecificationExecutor<ProfessionalBusiness> {

    List<ProfessionalBusiness> findByCreatedByUser_IdOrderByCreatedAtDesc(Integer userId);

    List<ProfessionalBusiness> findByVerifiedFalseOrderByCreatedAtDesc();

    long countByVerifiedFalseAndActiveFalse();

    long countByVerifiedTrueAndActiveTrue();

    long countByActiveFalse();

}
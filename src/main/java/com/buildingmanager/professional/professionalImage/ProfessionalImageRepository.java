package com.buildingmanager.professional.professionalImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessionalImageRepository extends JpaRepository<ProfessionalImage, Integer> {

    List<ProfessionalImage> findByProfessional_IdOrderByPrimaryImageDescCreatedAtDesc(Integer professionalId);

    List<ProfessionalImage> findByProfessional_Id(Integer professionalId);
}

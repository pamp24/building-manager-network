package com.buildingmanager.professional.professionalReview;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalReviewRepository extends JpaRepository<ProfessionalReview, Integer> {

    List<ProfessionalReview> findByProfessional_IdOrderByCreatedAtDesc(Integer professionalId);

    Optional<ProfessionalReview> findByProfessionalAndUser(
            ProfessionalBusiness professional,
            User user
    );

    boolean existsByProfessionalAndUser(
            ProfessionalBusiness professional,
            User user
    );

    long countByProfessional(ProfessionalBusiness professional);

    long count();
}

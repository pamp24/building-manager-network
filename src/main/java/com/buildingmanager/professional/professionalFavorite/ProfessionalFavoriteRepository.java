package com.buildingmanager.professional.professionalFavorite;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalFavoriteRepository
        extends JpaRepository<ProfessionalFavorite, Integer> {

    boolean existsByUserAndProfessional(
            User user,
            ProfessionalBusiness professional
    );

    Optional<ProfessionalFavorite> findByUserAndProfessional(
            User user,
            ProfessionalBusiness professional
    );

    List<ProfessionalFavorite> findAllByUser(User user);
}

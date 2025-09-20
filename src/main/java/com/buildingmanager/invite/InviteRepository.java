package com.buildingmanager.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    boolean existsByApartmentIdAndRoleAndStatus(Integer apartmentId, String role, InviteStatus status);
    Optional<Invite> findByToken(String token);


}


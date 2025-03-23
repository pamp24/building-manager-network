package com.BuildingManager.building_manager_network.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token , Integer> {

    Optional<Token> findByToken(String token);
}

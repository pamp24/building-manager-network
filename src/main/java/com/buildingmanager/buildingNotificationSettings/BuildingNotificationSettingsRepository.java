package com.buildingmanager.buildingNotificationSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingNotificationSettingsRepository extends JpaRepository<BuildingNotificationSettings, Integer> {
    Optional<BuildingNotificationSettings> findByBuilding_Id(Integer buildingId);
}

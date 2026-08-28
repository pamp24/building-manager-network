package com.buildingmanager.notificationPreference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Integer> {

    Optional<NotificationPreference> findByUser_Id(Integer userId);
}

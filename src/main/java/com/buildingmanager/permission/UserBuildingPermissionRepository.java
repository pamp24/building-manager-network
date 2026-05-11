package com.buildingmanager.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBuildingPermissionRepository extends JpaRepository<UserBuildingPermission, Integer> {
    List<UserBuildingPermission> findByUserId(Integer userId);
    Optional<UserBuildingPermission> findByUserIdAndBuildingId(Integer userId, Integer buildingId);
    void deleteByUserId(Integer userId);

    List<UserBuildingPermission> findByBuilding_Id(Integer buildingId);

}
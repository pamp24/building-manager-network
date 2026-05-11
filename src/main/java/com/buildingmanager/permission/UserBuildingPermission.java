package com.buildingmanager.permission;

import com.buildingmanager.building.Building;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "user_building_permission",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "building_id"})
        }
)
public class UserBuildingPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildingPermissionLevel permissionLevel;
}

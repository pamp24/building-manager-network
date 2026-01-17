package com.buildingmanager.calendar;

import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "CalendarEntity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "calendar")
public class Calendar extends BaseEntity {

    private String title;
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String colorPrimary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdByUser;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean pinned = false;


}

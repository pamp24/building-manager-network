package com.buildingmanager.calendar;

import com.buildingmanager.building.Building;
import com.buildingmanager.user.User;
import org.springframework.stereotype.Component;

@Component
public class CalendarMapper {

    public CalendarDTO toDTO(Calendar entity) {
        if (entity == null) return null;
        return CalendarDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .colorPrimary(entity.getColorPrimary())
                .buildingId(entity.getBuilding() != null ? entity.getBuilding().getId() : null)
                .active(entity.isActive())
                .build();
    }

    public Calendar toEntity(CalendarDTO dto) {
        if (dto == null) return null;

        Building building = null;
        if (dto.getBuildingId() != null) {
            building = Building.builder().id(dto.getBuildingId()).build();
        }

        User user = null;
        if (dto.getCreatedById() != null) {
            user = User.builder().id(dto.getCreatedById()).build();
        }

        return Calendar.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .colorPrimary(dto.getColorPrimary())
                .building(building)
                .active(dto.isActive())
                .build();
    }
}

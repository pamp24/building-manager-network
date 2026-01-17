package com.buildingmanager.calendar;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String colorPrimary;
    private Integer buildingId;
    private Integer createdById;
    private boolean active;

    private boolean pinned;
}

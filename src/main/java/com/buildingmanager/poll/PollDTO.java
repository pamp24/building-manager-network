package com.buildingmanager.poll;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private boolean multipleChoice;
    private Integer buildingId;
    private List<PollOptionDTO> options;

    private String leadingOption;

}

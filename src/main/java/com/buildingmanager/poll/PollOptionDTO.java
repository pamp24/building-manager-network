package com.buildingmanager.poll;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionDTO {
    private Integer id;
    private String text;
    private int votes;
    private Integer position;
}
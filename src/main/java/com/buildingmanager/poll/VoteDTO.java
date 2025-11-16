package com.buildingmanager.poll;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteDTO {
    private Integer userId;
    private String userFullName;
    private String optionText;
    private LocalDateTime voteDate;

    private Integer optionNumber;
}

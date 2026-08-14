package com.buildingmanager.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {

    private Integer userId;
    private String fullName;
    private String profileImageUrl;
}

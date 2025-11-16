package com.buildingmanager.poll;


import com.buildingmanager.building.Building;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PollMapper {

    //Entity -> DTO
    public PollDTO toDTO(Poll entity) {
        if (entity == null) return null;

        String leading = null;

        if (entity.getOptions() != null && !entity.getOptions().isEmpty()) {
            leading = entity.getOptions().stream()
                    .max(Comparator.comparingInt(PollOption::getVotes))
                    .map(PollOption::getText)
                    .orElse(null);
        }

        return PollDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .active(entity.isActive())
                .multipleChoice(entity.isMultipleChoice())
                .buildingId(entity.getBuilding() != null ? entity.getBuilding().getId() : null)
                .options(toOptionDTOList(entity.getOptions()))
                .leadingOption(leading)
                .build();
    }

    //DTO -> Entity
    public Poll toEntity(PollDTO dto) {
        if (dto == null) return null;

        Building building = null;
        if (dto.getBuildingId() != null) {
            building = Building.builder().id(dto.getBuildingId()).build();
        }

        Poll poll = Poll.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .active(dto.isActive())
                .multipleChoice(dto.isMultipleChoice())
                .building(building)
                .build();

        if (dto.getOptions() != null) {
            List<PollOption> options = dto.getOptions().stream()
                    .map(o -> toOptionEntity(o, poll))
                    .collect(Collectors.toList());
            poll.setOptions(options);
        }

        return poll;
    }

    //Option Entity -> DTO
    public PollOptionDTO toOptionDTO(PollOption option) {
        if (option == null) return null;

        return PollOptionDTO.builder()
                .id(option.getId())
                .text(option.getText())
                .votes(option.getVotes())
                .build();
    }

    //Option DTO -> Entity
    public PollOption toOptionEntity(PollOptionDTO dto, Poll poll) {
        if (dto == null) return null;

        return PollOption.builder()
                .id(dto.getId())
                .text(dto.getText())
                .votes(dto.getVotes())
                .poll(poll)
                .build();
    }

    //Helper methods
    private List<PollOptionDTO> toOptionDTOList(List<PollOption> options) {
        if (options == null) return List.of();

        return options.stream()
                .sorted((a, b) -> {
                    if (a.getPosition() == null || b.getPosition() == null) return 0;
                    return a.getPosition().compareTo(b.getPosition());
                })
                .map(this::toOptionDTO)
                .collect(Collectors.toList());
    }
}

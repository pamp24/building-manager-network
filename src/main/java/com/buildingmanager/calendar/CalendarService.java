package com.buildingmanager.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository repository;
    private final CalendarMapper mapper;

    public List<CalendarDTO> getByBuilding(Integer buildingId) {
        return repository.findByBuildingIdAndActiveTrue(buildingId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    public CalendarDTO create(CalendarDTO dto) {
        Calendar entity = mapper.toEntity(dto);
        entity.setActive(true);
        Calendar saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    public void delete(Integer id) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        existing.setActive(false);
        repository.save(existing);
    }

    public CalendarDTO update(Integer id, CalendarDTO dto) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setColorPrimary(dto.getColorPrimary());

        Calendar updated = repository.save(existing);
        return mapper.toDTO(updated);
    }


}

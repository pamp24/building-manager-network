package com.buildingmanager.calendar;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository repository;
    private final CalendarMapper mapper;

    public List<CalendarDTO> getByBuilding(Integer buildingId) {
        return repository.findByBuildingPinnedFirst(buildingId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    public CalendarDTO create(CalendarDTO dto) {
        Calendar entity = mapper.toEntity(dto);
        entity.setActive(true);

        // αν δημιουργηθεί pinned -> ξεκαρφίτσωσε άλλα
        if (entity.isPinned()) {
            unpinAllInBuilding(entity.getBuilding().getId());
        }

        Calendar saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    public CalendarDTO pin(Integer id, boolean pinned) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        existing.setPinned(pinned);

        // Αν θες μόνο ένα pinned τη φορά:
        // αν pinned=true → ξεκαρφίτσωσε όλα τα άλλα της ίδιας πολυκατοικίας
        if (pinned && existing.getBuilding() != null) {
            Integer buildingId = existing.getBuilding().getId();
            List<Calendar> events = repository.findByBuildingPinnedFirst(buildingId);
            for (Calendar e : events) {
                if (!e.getId().equals(existing.getId()) && e.isPinned()) {
                    e.setPinned(false);
                }
            }
            repository.saveAll(events);
        }

        Calendar saved = repository.save(existing);
        return mapper.toDTO(saved);
    }

    private void unpinAllInBuilding(Integer buildingId) {
        List<Calendar> list = repository.findByBuildingIdAndActiveTrue(buildingId);
        list.forEach(e -> e.setPinned(false));
        repository.saveAll(list);
    }

    public void delete(Integer id) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        existing.setActive(false);
        existing.setPinned(false);
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

        // αν το update μπορεί να αλλάξει pinned:
        if (dto.isPinned() && !existing.isPinned()) {
            unpinAllInBuilding(existing.getBuilding().getId());
        }
        existing.setPinned(dto.isPinned());

        Calendar updated = repository.save(existing);
        return mapper.toDTO(updated);
    }


}

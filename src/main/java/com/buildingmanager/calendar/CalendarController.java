package com.buildingmanager.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/building/{buildingId}")
    public List<CalendarDTO> getByBuilding(@PathVariable Integer buildingId) {
        return calendarService.getByBuilding(buildingId);
    }

    @PostMapping
    public CalendarDTO create(@RequestBody CalendarDTO dto) {
        return calendarService.create(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        calendarService.delete(id);
    }
    @PutMapping("/{id}")
    public CalendarDTO update(@PathVariable Integer id, @RequestBody CalendarDTO dto) {
        return calendarService.update(id, dto);
    }
}

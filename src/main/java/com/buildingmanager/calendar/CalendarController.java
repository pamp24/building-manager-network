package com.buildingmanager.calendar;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/building/{buildingId}")
    public List<CalendarDTO> getByBuilding(@PathVariable Integer buildingId, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return calendarService.getByBuilding(buildingId, user.getId());
    }

    @PostMapping
    public CalendarDTO create(@RequestBody CalendarDTO dto, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return calendarService.create(dto, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        calendarService.delete(id, user);
    }

    @PutMapping("/{id}")
    public CalendarDTO update(@PathVariable Integer id, @RequestBody CalendarDTO dto, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return calendarService.update(id, dto, user);
    }

    @PutMapping("/{id}/pin")
    public CalendarDTO pin(@PathVariable Integer id, @RequestParam boolean pinned, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return calendarService.pin(id, pinned, user);
    }
}

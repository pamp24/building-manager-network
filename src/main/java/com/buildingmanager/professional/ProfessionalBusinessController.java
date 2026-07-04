package com.buildingmanager.professional;

import com.buildingmanager.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalBusinessController {

    private final ProfessionalBusinessService service;

    @GetMapping
    public List<ProfessionalBusinessDTO> search(
            @RequestParam(required = false) ProfessionalCategory category,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area
    ) {
        return service.search(category, country, region, city, area);
    }

    @PostMapping("/register")
    public ProfessionalBusinessDTO register(
            @Valid @RequestBody ProfessionalBusinessRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return service.register(request, user);
    }

    @GetMapping("/my")
    public List<ProfessionalBusinessDTO> getMyBusinesses(
            org.springframework.security.core.Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return service.getMyBusinesses(user);
    }

    @GetMapping("/pending")
    public List<ProfessionalBusinessDTO> getPendingApproval(
            org.springframework.security.core.Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return service.getPendingApproval(user);
    }

    @PatchMapping("/{id}/approve")
    public ProfessionalBusinessDTO approve(
            @PathVariable Integer id,
            org.springframework.security.core.Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return service.approve(id, user);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivate(
            @PathVariable Integer id,
            org.springframework.security.core.Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        service.deactivate(id, user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalBusinessDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalBusinessDTO> update(
            @PathVariable Integer id,
            @RequestBody ProfessionalBusinessRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                service.update(id, request, user)
        );
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<ProfessionalAdminStatsDTO> getAdminStats(Authentication authentication) {
        System.out.println("AUTH = " + authentication.getAuthorities());

        return ResponseEntity.ok(service.getAdminStats());
    }

    @GetMapping("/admin/businesses")
    public ResponseEntity<Page<ProfessionalBusinessDTO>> getAdminBusinesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                service.getAdminBusinesses(page, size, user)
        );
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteBusiness(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        service.deleteBusiness(id, user);

        return ResponseEntity.noContent().build();
    }
}
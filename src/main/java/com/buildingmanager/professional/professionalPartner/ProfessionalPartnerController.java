package com.buildingmanager.professional.professionalPartner;

import com.buildingmanager.professional.ProfessionalBusinessDTO;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professional-partners")
@RequiredArgsConstructor
public class ProfessionalPartnerController {

    private final ProfessionalPartnerService service;

    @PostMapping("/buildings/{buildingId}/professionals/{professionalId}")
    public ResponseEntity<Void> addPartner(
            @PathVariable Integer buildingId,
            @PathVariable Integer professionalId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        service.addPartner(buildingId, professionalId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/buildings/{buildingId}/professionals/{professionalId}")
    public ResponseEntity<Void> removePartner(
            @PathVariable Integer buildingId,
            @PathVariable Integer professionalId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        service.removePartner(buildingId, professionalId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buildings/{buildingId}")
    public ResponseEntity<List<ProfessionalBusinessDTO>> getPartners(
            @PathVariable Integer buildingId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(service.getPartners(buildingId, user));
    }
}
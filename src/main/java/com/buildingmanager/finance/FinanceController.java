package com.buildingmanager.finance;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final ApartmentRepository apartmentRepository;

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<BuildingFinanceDTO> getBuildingFinance(
            @PathVariable Integer buildingId,
            Authentication auth
    ) {
        User user = (User) auth.getPrincipal();
        BuildingFinanceDTO dto = financeService.getBuildingFinance(buildingId, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my-building")
    public ResponseEntity<BuildingFinanceDTO> getMyBuildingFinance(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<Apartment> residentApts = apartmentRepository.findByResident_Id(user.getId());
        if (!residentApts.isEmpty()) {
            Integer buildingId = residentApts.get(0).getBuilding().getId();
            return ResponseEntity.ok(financeService.getBuildingFinance(buildingId, user));
        }
        List<Apartment> ownerApts = apartmentRepository.findByOwner_Id(user.getId());
        if (!ownerApts.isEmpty()) {
            Integer buildingId = ownerApts.get(0).getBuilding().getId();
            return ResponseEntity.ok(financeService.getBuildingFinance(buildingId, user));
        }
        return ResponseEntity.badRequest().build();
    }
}

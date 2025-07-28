package com.buildingmanager.apartment;

import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apartments")
@RequiredArgsConstructor
@Tag(name = "Apartment")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final ApartmentMapper apartmentMapper;

    @PostMapping
    public ResponseEntity<Integer> saveApartment(
            @Valid @RequestBody ApartmentRequest request,
            Authentication connectedUser
    ){
        return ResponseEntity.ok((Integer) apartmentService.save(request, connectedUser));
    }

    @GetMapping("/building/{building-id}")
    public ResponseEntity<PageResponse<ApartmentResponse>> findAllApartmentsByBuilding(
            @PathVariable("building-id") Integer buildingId,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(apartmentService.findAllApartmentsByBuilding(buildingId, page, size, connectedUser));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> saveMultipleApartments(
            @Valid @RequestBody List<ApartmentRequest> requests,
            Authentication connectedUser
    ) {
        requests.forEach(r -> System.out.println("Received: " + r));
        apartmentService.saveAll(requests, connectedUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/myApartment")
    public ResponseEntity<ApartmentResponse> getMyApartment(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Apartment apartment = apartmentService.findByUser(user.getId());
        ApartmentResponse response = apartmentMapper.toApartmentResponse(apartment, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/same-building")
    public ResponseEntity<List<ApartmentResponse>> getApartmentsInSameBuilding(Authentication authentication) {
        List<ApartmentResponse> result = apartmentService.getApartmentsInSameBuilding(authentication);
        return ResponseEntity.ok(result);
    }


}

package com.buildingmanager.apartment;

import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/apartments")
@RequiredArgsConstructor
@Tag(name = "Apartment")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final ApartmentMapper apartmentMapper;
    private final ApartmentRepository apartmentRepository;
    private static final Logger log = LoggerFactory.getLogger(ApartmentController.class);


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

    @GetMapping("/my-apartments")
    public ResponseEntity<List<ApartmentResponse>> getMyApartments(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Apartment> apartments = apartmentService.findByUser(user.getId());

        if (apartments.isEmpty()) {
            log.error("Apartments not found for user id: " + user.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }

        List<ApartmentResponse> response = apartments.stream()
                .map(ap -> apartmentMapper.toApartmentResponse(ap, user.getId()))
                .toList();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/same-building")
    public ResponseEntity<List<ApartmentResponse>> getApartmentsInSameBuilding(Authentication authentication) {
        List<ApartmentResponse> result = apartmentService.getApartmentsInSameBuilding(authentication);

        if (result.isEmpty()) {
            // Προαιρετικά: επιστρέφουμε 404 αν δεν υπάρχει διαμέρισμα
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.emptyList());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/building/{buildingId}/list")
    public ResponseEntity<List<ApartmentResponse>> getApartmentsByBuilding(
            @PathVariable Integer buildingId
    ) {
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);
        List<ApartmentResponse> response = apartments.stream()
                .map(apartment -> apartmentMapper.toApartmentResponse(apartment, null))
                .toList();
        return ResponseEntity.ok(response);
    }


}

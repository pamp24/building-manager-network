package com.buildingmanager.apartment;

import com.buildingmanager.common.PageResponse;
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

    private final ApartmentService service;

    @PostMapping
    public ResponseEntity<Integer> saveApartment(
            @Valid @RequestBody ApartmentRequest request,
            Authentication connectedUser
    ){
        return ResponseEntity.ok((Integer) service.save(request, connectedUser));
    }

    @GetMapping("/building/{building-id}")
    public ResponseEntity<PageResponse<ApartmentResponse>> findAllApartmentsByBuilding(
            @PathVariable("building-id") Integer buildingId,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.findAllApartmentsByBuilding(buildingId, page, size, connectedUser));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> saveMultipleApartments(
            @Valid @RequestBody List<ApartmentRequest> requests,
            Authentication connectedUser
    ) {
        requests.forEach(r -> System.out.println("Received: " + r));
        service.saveAll(requests, connectedUser);
        return ResponseEntity.ok().build();
    }


}

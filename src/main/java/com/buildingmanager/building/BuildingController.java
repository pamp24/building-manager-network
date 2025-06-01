package com.buildingmanager.building;

import com.buildingmanager.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/buildings")
@RequiredArgsConstructor
@Tag(name = "Building")
public class BuildingController {


    private final BuildingService service;

    @PostMapping
    public ResponseEntity<Integer> saveBuilding(
            @Valid @RequestBody BuildingRequest request,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.save(request, connectedUser));
    }

    @GetMapping("{building-id}")
    public ResponseEntity<BuildingResponse> findBuildingById(
            @PathVariable("building-id") Integer buildingId
    ){
        return ResponseEntity.ok(service.findById(buildingId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BuildingResponse>> findAllBuildings(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser

            ){
        return ResponseEntity.ok(service.findAllBuildings(page, size, connectedUser));
    }
    @GetMapping("/manager")
    public ResponseEntity<PageResponse<BuildingResponse>> findAllBuildingsByManager(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.findAllBuildingsByManager(page, size, connectedUser));
    }
}

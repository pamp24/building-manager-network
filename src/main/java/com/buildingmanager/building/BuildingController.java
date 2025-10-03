package com.buildingmanager.building;


import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
@Tag(name = "Building")
public class    BuildingController {
    private final UserService userService;
    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<Integer> saveBuilding(
            @Valid @RequestBody BuildingRequest request,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(buildingService.save(request, connectedUser));
    }

    @GetMapping("{building-id}")
    public ResponseEntity<BuildingResponse> findBuildingById(
            @PathVariable("building-id") Integer buildingId
    ){
        return ResponseEntity.ok(buildingService.findById(buildingId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BuildingResponse>> findAllBuildings(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser

            ){
        return ResponseEntity.ok(buildingService.findAllBuildings(page, size, connectedUser));
    }
    @GetMapping("/manager")
    public ResponseEntity<PageResponse<BuildingResponse>> findAllBuildingsByManager(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(buildingService.findAllBuildingsByManager(page, size, connectedUser));
    }

    @GetMapping("/myBuilding")
    public ResponseEntity<BuildingResponse> getMyBuilding(Authentication auth) {
        return buildingService.findBuildingOfCurrentUser(auth)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
    @GetMapping("/my-buildings")
    public ResponseEntity<List<BuildingResponse>> getMyBuildings(Authentication authentication) {
        List<BuildingResponse> result = buildingService.getMyBuildings(authentication);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication auth) {
        buildingService.deleteBuilding(id, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{buildingId}/manager")
    public ResponseEntity<ManagerDTO> getManager(@PathVariable Integer buildingId) {
        return ResponseEntity.ok(buildingService.getManagerDTO(buildingId));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<BuildingDTO> updateBuilding(
            @PathVariable Integer id,
            @RequestBody BuildingDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(buildingService.updateBuilding(id, dto, authentication));
    }
    @GetMapping("/my-managed-buildings")
    public List<ManagedBuildingDTO> getManagedBuildings(@AuthenticationPrincipal User user) {
        return buildingService.getManagedBuildings(user.getId());
    }

}

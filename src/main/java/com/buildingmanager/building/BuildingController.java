package com.buildingmanager.building;


import com.buildingmanager.buildingMember.BuildingMemberService;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
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

    private final BuildingService buildingService;
    private final BuildingMemberService buildingMemberService;

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
    public ResponseEntity<List<BuildingDTO>> getMyBuildings(Authentication authentication) {
        return ResponseEntity.ok(buildingService.getMyBuildingsDTO(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication auth) {
        buildingService.deleteBuilding(id, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/draft")
    public ResponseEntity<Void> deleteDraft(@PathVariable Integer id, Authentication auth) {
        buildingService.deleteDraftBuilding(id, auth);
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

    @PostMapping("/join-by-code")
    public ResponseEntity<JoinBuildingResponseDTO> joinByCode(@RequestParam String code, Authentication auth) {
        Integer buildingId = buildingMemberService.joinByBuildingCode(code, auth);
        return ResponseEntity.ok(new JoinBuildingResponseDTO(buildingId));
    }

    @PostMapping("/self")
    public ResponseEntity<Integer> createSelf(@Valid @RequestBody BuildingRequest request, Authentication auth) {
        return ResponseEntity.ok(buildingService.createSelfManaged(request, auth));
    }

    @PostMapping("/company")
    public ResponseEntity<Integer> createCompany(@Valid @RequestBody BuildingRequest request, Authentication auth) {
        return ResponseEntity.ok(buildingService.createCompanyManaged(request, auth));
    }

    @GetMapping("/pm/my-company-buildings")
    public ResponseEntity<List<BuildingResponse>> myCompanyBuildings(Authentication auth) {
        return ResponseEntity.ok(buildingService.getMyCompanyBuildings(auth));
    }

}

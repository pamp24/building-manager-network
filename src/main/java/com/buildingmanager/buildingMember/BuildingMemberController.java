package com.buildingmanager.buildingMember;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/building-members")
@RequiredArgsConstructor
public class BuildingMemberController {

    private final BuildingMemberService buildingMemberService;

    @PostMapping
    public ResponseEntity<BuildingMember> addMember(
            @RequestParam Integer buildingId,
            @RequestParam Integer userId,
            @RequestParam Integer roleId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(buildingMemberService.addMember(buildingId, userId, roleId, status));
    }

    @GetMapping("/by-building/{buildingId}")
    public ResponseEntity<List<BuildingMemberDTO>> getMembersByBuilding(@PathVariable Integer   buildingId) {
        return ResponseEntity.ok(buildingMemberService.getMembersByBuilding(buildingId));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<BuildingMember>> getMembersByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(buildingMemberService.getMembersByUser(userId));
    }


    @PostMapping("/{memberId}/assign-apartment")
    public ResponseEntity<Void> assignApartment(
            @PathVariable Integer memberId,
            @RequestBody AssignApartmentRequest req,
            Authentication auth
    ) {
        buildingMemberService.assignApartment(memberId, req, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteMember/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Integer memberId,
            Authentication auth
    ) {
        buildingMemberService.deleteMember(memberId, auth);
        return ResponseEntity.noContent().build();
    }
}

package com.buildingmanager.user;

import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleDTO;
import com.buildingmanager.role.RoleRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.buildingmanager.role.RoleService;


import java.util.Optional;



@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Integer userId,
            @RequestParam String roleName) {

        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Δεν βρέθηκε ο Ρόλος");
        }

        return userRepository.findById(userId)
                .map(user -> {
                    user.setRole(roleOpt.get());
                    userRepository.save(user);
                    return ResponseEntity.ok("Ο ρόλος ενημερώθηκε επιτυχώς");
                })
                .orElse(ResponseEntity.badRequest().body("Δεν βρέθηκε ο χρήστης"));
    }

    @GetMapping("/{userId}/role")
    public ResponseEntity<RoleDTO> getUserRole(@PathVariable Integer userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getRole() == null) {
            return ResponseEntity.notFound().build();
        }

        Role role = userOpt.get().getRole();
        return ResponseEntity.ok(new RoleDTO(role.getName()));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO dto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    // Ενημέρωση πεδίων
                    user.setFirstName(dto.getFirstName());
                    user.setLastName(dto.getLastName());
                    user.setDateOfBirth(dto.getDateOfBirth());
                    user.setPhoneNumber(dto.getPhoneNumber());
                    user.setProfileImageUrl(dto.getProfileImageUrl());
                    user.setAddress1(dto.getAddress1());
                    user.setAddressNumber1(dto.getAddressNumber1());
                    user.setAddress2(dto.getAddress2());
                    user.setAddressNumber2(dto.getAddressNumber2());
                    user.setCountry(dto.getCountry());
                    user.setState(dto.getState());
                    user.setCity(dto.getCity());
                    user.setRegion(dto.getRegion());
                    user.setPostalCode(dto.getPostalCode());
                    user.setDateOfBirth(dto.getDateOfBirth());
                    userRepository.save(user);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/roles/assign")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Integer userId,
            @RequestParam String roleName) {

        Optional<Role> roleOpt = roleService.findByName(roleName);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Ο ρόλος δεν βρέθηκε");
        }

        boolean updated = userService.updateUserRole(userId, roleOpt.get());
        if (updated) {
            return ResponseEntity.ok("Ο ρόλος ανατέθηκε επιτυχώς");
        } else {
            return ResponseEntity.badRequest().body("Ο χρήστης δεν βρέθηκε");
        }
    }




}

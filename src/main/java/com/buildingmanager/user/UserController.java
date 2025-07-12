package com.buildingmanager.user;

import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleDTO;
import com.buildingmanager.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Integer userId,
            @RequestParam String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Δεν βρέθηκε ο Ρόλος");
        }
        Role role = roleOpt.get();
        boolean updated = userService.updateUserRole(userId, role);
        if (updated) {
            return ResponseEntity.ok("Ο ρόλος ενημερώθηκε επιτυχώς");
        } else {
            return ResponseEntity.badRequest().body("Δεν βρέθηκε ο χρήστης");
        }
    }

    @PostMapping("/{userId}/roles/assign")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Integer userId,
            @RequestParam String roleName
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε ο Χρήστης"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε ο Ρόλος"));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
        return ResponseEntity.ok("Ο ρόλος ανατέθει επιτυχώς");
    }

    @PostMapping("/{userId}/roles/remove")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Integer userId,
            @RequestParam String roleName
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            userRepository.save(user);
        }

        return ResponseEntity.ok("Ο ρόλος αφαιρέθηκε επιτυχώς");
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<RoleDTO>> getUserRoles(@PathVariable Integer userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        List<RoleDTO> rolesDto = user.getRoles().stream()
                .map(role -> new RoleDTO(role.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(rolesDto);
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



}

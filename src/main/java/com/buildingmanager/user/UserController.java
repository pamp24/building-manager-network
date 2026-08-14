package com.buildingmanager.user;

import com.buildingmanager.audit.AuditAction;
import com.buildingmanager.audit.Auditable;
import com.buildingmanager.exceptions.UserNotFoundException;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleDTO;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.role.RoleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    @PutMapping("/{userId}/role")
    @Auditable(action = AuditAction.PERMISSION_CHANGE)
    public ResponseEntity<?> updateUserRole(
            @PathVariable Integer userId,
            @RequestParam String roleName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id '" + userId + "' not found"));

        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok("Ο ρόλος ενημερώθηκε επιτυχώς");
    }

    @GetMapping("/{userId}/role")
    public ResponseEntity<RoleDTO> getUserRole(@PathVariable Integer userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id '" + userId + "' not found"));

        if (user.getRole() == null) {
            throw new EntityNotFoundException("User has no role assigned");
        }

        return ResponseEntity.ok(new RoleDTO(user.getRole().getName()));
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.fullName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .profileImageUrl(user.getProfileImageUrl())
                        .city(user.getCity())
                        .region(user.getRegion())
                        .createdDate(user.getCreatedDate())
                        .lastLoginDate(user.getLastLoginDate())
                        .enabled(user.isEnabled())
                        .accountLocked(!user.isAccountNonLocked())
                        .deleted(user.isDeleted())
                        .role(user.getRole() != null ? user.getRole().getName() : null)
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<UserDTO> setUserEnabled(
            @PathVariable Integer userId,
            @RequestParam boolean enabled,
            Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof User currentUser
                && currentUser.getId().equals(userId) && !enabled) {
            throw new AccessDeniedException("Δεν μπορείτε να απενεργοποιήσετε τον λογαριασμό σας");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id '" + userId + "' not found"));

        user.setEnable(enabled);
        userRepository.save(user);

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.fullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .city(user.getCity())
                .region(user.getRegion())
                .createdDate(user.getCreatedDate())
                .lastLoginDate(user.getLastLoginDate())
                .enabled(user.isEnabled())
                .accountLocked(!user.isAccountNonLocked())
                .deleted(user.isDeleted())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .build();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable Integer userId,
            Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof User currentUser
                && currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Δεν μπορείτε να διαγράψετε τον λογαριασμό σας");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id '" + userId + "' not found"));

        user.setDeleted(true);
        user.setEnable(false);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody com.buildingmanager.user.UserUpdateDTO dto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email '" + email + "' not found"));

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
    }

    @PostMapping("/me/profile-image")
    public ResponseEntity<Map<String,String>> upload(@RequestParam("file") MultipartFile file,
                                                     Authentication auth) {
        log.debug("Upload profile-image: file={}, size={}", file.getOriginalFilename(), file.getSize());
        User user = (User) auth.getPrincipal();
        String url = userService.uploadProfileImage(file, user.getId());
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }

    @PostMapping("/{userId}/roles/assign")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Integer userId,
            @RequestParam String roleName) {

        Role role = roleService.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        boolean updated = userService.updateUserRole(userId, role);
        if (!updated) {
            throw new UserNotFoundException("User with id '" + userId + "' not found");
        }

        return ResponseEntity.ok("Ο ρόλος ανατέθηκε επιτυχώς");
    }

    @GetMapping("/same-building")
    public ResponseEntity<List<UserTableDto>> getUsersInSameBuilding(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<UserTableDto> result = userService.getUsersInSameBuilding(currentUser.getId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteUserToBuilding(
            @RequestParam String email,
            @RequestParam Integer buildingId,
            Authentication auth
    ) {
        userService.inviteUserToBuilding(email, buildingId, auth);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/preferences/professionals-favorites-only")
    public ResponseEntity<Void> updateProfessionalsFavoritesOnly(
            @RequestParam boolean enabled,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        userService.updateProfessionalsFavoritesOnly(user.getId(), enabled);

        return ResponseEntity.ok().build();
    }

}

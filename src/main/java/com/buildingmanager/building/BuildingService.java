package com.buildingmanager.building;


import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.audit.AuditAction;
import com.buildingmanager.audit.Auditable;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final RoleRepository roleRepository;
    private final BuildingPermissionService buildingPermissionService;
    private final UserBuildingPermissionRepository userBuildingPermissionRepository;
    private final Validator validator;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private User freshUser(Authentication auth) {
        User principal = (User) auth.getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE)
    public Integer createSelfManaged(BuildingRequest request, Authentication connectedUser) {
        User currentUser = (User) connectedUser.getPrincipal();

        Building building = buildingMapper.toBuilding(request);
        building.setBuildingCode(generateBuildingCode());

        // self
        building.setManager(currentUser);
        building.setCompany(null);
        building.setPropertyManager(null);

        Building saved = buildingRepository.save(building);

        Role bmRole = roleRepository.findByName("BuildingManager")
                .orElseThrow(() -> new RuntimeException("Role 'BuildingManager' not found"));

        buildingMemberRepository.save(
                BuildingMember.builder()
                        .building(saved)
                        .user(currentUser)
                        .role(bmRole)
                        .status(BuildingMemberStatus.JOINED)
                        .build()
        );

        // global role upgrade
        currentUser.setRole(bmRole);
        userRepository.save(currentUser);

        return saved.getId();
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE)
    public Integer createCompanyManaged(BuildingRequest request, Authentication auth) {
        User currentUser = freshUser(auth);

        if (currentUser.getRole() == null || !"PropertyManager".equals(currentUser.getRole().getName())) {
            throw new AccessDeniedException("Μόνο PropertyManager μπορεί να δημιουργήσει πολυκατοικία (company).");
        }
        if (currentUser.getCompany() == null) {
            throw new AccessDeniedException("Ο PropertyManager πρέπει πρώτα να έχει εταιρία.");
        }

        Building building = buildingMapper.toBuilding(request);
        building.setBuildingCode(generateBuildingCode());

        building.setCompany(currentUser.getCompany());
        building.setPropertyManager(currentUser);

        // ΣΗΜΑΝΤΙΚΟ: μην βάζεις manager από request εδώ
        building.setManager(null);

        Building saved = buildingRepository.save(building);

        Role pmRole = roleRepository.findByName("PropertyManager")
                .orElseThrow(() -> new RuntimeException("Role PropertyManager not found"));

        buildingMemberRepository.save(
                BuildingMember.builder()
                        .building(saved)
                        .user(currentUser)
                        .role(pmRole)
                        .status(BuildingMemberStatus.JOINED)
                        .build()
        );

        return saved.getId();
    }

    @Transactional
    public List<Integer> createCompanyBulk(List<BuildingRequest> requests, Authentication auth) {
        User user = freshUser(auth);
        if (user.getRole() == null || !"PropertyManager".equals(user.getRole().getName()) || user.getCompany() == null) {
            throw new AccessDeniedException("A PropertyManager with a company is required");
        }

        List<BulkBuildingValidationException.Error> errors = new ArrayList<>();
        if (requests == null || requests.isEmpty() || requests.size() > 500) {
            errors.add(new BulkBuildingValidationException.Error(0, "rows", "Provide between 1 and 500 rows"));
            throw new BulkBuildingValidationException(errors);
        }
        Map<List<String>, Integer> addresses = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            BuildingRequest r = requests.get(i);
            int row = i + 1;
            if (r == null) {
                errors.add(new BulkBuildingValidationException.Error(row, "row", "Row must not be null"));
                continue;
            }
            validator.validate(r).forEach(v -> errors.add(new BulkBuildingValidationException.Error(row, v.getPropertyPath().toString(), v.getMessage())));
            if (r.id() != null) errors.add(new BulkBuildingValidationException.Error(row, "id", "Must be omitted or null"));
            if (r.managerId() != null) errors.add(new BulkBuildingValidationException.Error(row, "managerId", "Must be omitted or null"));
            try {
                if (Integer.parseInt(r.floors()) < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                errors.add(new BulkBuildingValidationException.Error(row, "floors", "Must be a non-negative integer"));
            }
            if (r.apartmentsNum() != null && r.apartmentsNum() < 0) errors.add(new BulkBuildingValidationException.Error(row, "apartmentsNum", "Must be non-negative"));
            if (r.parkingSpacesNum() != null && r.parkingSpacesNum() < 0) errors.add(new BulkBuildingValidationException.Error(row, "parkingSpacesNum", "Must be non-negative"));
            if (r.storageNum() != null && r.storageNum() < 0) errors.add(new BulkBuildingValidationException.Error(row, "storageNum", "Must be non-negative"));
            if (!Double.isFinite(r.sqMetersTotal()) || r.sqMetersTotal() < 0) errors.add(new BulkBuildingValidationException.Error(row, "sqMetersTotal", "Must be a finite non-negative number"));
            if (!Double.isFinite(r.sqMetersCommonSpaces()) || r.sqMetersCommonSpaces() < 0 || r.sqMetersCommonSpaces() > r.sqMetersTotal()) errors.add(new BulkBuildingValidationException.Error(row, "sqMetersCommonSpaces", "Must be non-negative and no greater than total area"));
            if (r.heatingCapacityLitres() != null && (!Double.isFinite(r.heatingCapacityLitres()) || r.heatingCapacityLitres() < 0)) errors.add(new BulkBuildingValidationException.Error(row, "heatingCapacityLitres", "Must be a finite non-negative number"));
            if (r.heatingType() != null) {
                try { HeatingType.valueOf(r.heatingType().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException e) { errors.add(new BulkBuildingValidationException.Error(row, "heatingType", "Invalid heating type")); }
            }
            Map<String, String> strings = new HashMap<>();
            strings.put("name", r.name()); strings.put("street1", r.street1()); strings.put("stNumber1", r.stNumber1());
            strings.put("street2", r.street2()); strings.put("stNumber2", r.stNumber2()); strings.put("city", r.city());
            strings.put("region", r.region()); strings.put("postalCode", r.postalCode()); strings.put("country", r.country());
            strings.put("state", r.state()); strings.put("buildingDescription", r.buildingDescription());
            strings.forEach((field, value) -> { if (value != null && value.length() > 255) errors.add(new BulkBuildingValidationException.Error(row, field, "Must be at most 255 characters")); });
            if (r.street1() != null && !r.street1().isBlank() && r.stNumber1() != null && !r.stNumber1().isBlank() && r.city() != null && !r.city().isBlank() && r.postalCode() != null && !r.postalCode().isBlank() && r.country() != null && !r.country().isBlank()) {
                List<String> key = List.of(r.street1(), r.stNumber1(), r.city(), r.postalCode(), r.country()).stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();
                Integer previous = addresses.putIfAbsent(key, row);
                if (previous != null) errors.add(new BulkBuildingValidationException.Error(row, "street1", "Duplicate address in batch (row " + previous + ")"));
            }
        }
        if (!errors.isEmpty()) throw new BulkBuildingValidationException(errors);

        Role role = roleRepository.findByName("PropertyManager").orElseThrow(() -> new IllegalStateException("Role PropertyManager not found"));
        List<Integer> ids = new ArrayList<>();
        for (BuildingRequest request : requests) {
            Building building = buildingMapper.toBuilding(request);
            building.setBuildingCode(generateBuildingCode());
            building.setCompany(user.getCompany());
            building.setPropertyManager(user);
            building.setManager(null);
            Building saved = buildingRepository.save(building);
            buildingMemberRepository.save(BuildingMember.builder().building(saved).user(user).role(role).status(BuildingMemberStatus.JOINED).build());
            ids.add(saved.getId());
        }
        return ids;
    }


    @Transactional
    @Auditable(action = AuditAction.CREATE)
    public Integer save(BuildingRequest request, Authentication connectedUser) {
        User currentUser = (User) connectedUser.getPrincipal();

        //Μόνο PropertyManager
        if (currentUser.getRole() == null || currentUser.getRole().getId() != 5) {
            throw new AccessDeniedException("Μόνο PropertyManager μπορεί να δημιουργήσει πολυκατοικία.");
        }

        //Πρέπει να έχει εταιρία
        if (currentUser.getCompany() == null) {
            throw new AccessDeniedException("Ο PropertyManager πρέπει πρώτα να έχει εταιρία.");
        }

        Building building = buildingMapper.toBuilding(request);
        building.setBuildingCode(generateBuildingCode());

        //1) company_id = εταιρία του PM
        building.setCompany(currentUser.getCompany());

        //2) property_manager_id = ο PM που την δημιούργησε
        building.setPropertyManager(currentUser);

        //3) local building manager (building.manager) ΔΕΝ μπαίνει εδώ (μένει null)
        //building.setManager(null); // προαιρετικό, αν φοβάσαι ότι έρχεται από request

        Building savedBuilding = buildingRepository.save(building);

        // Local membership: ο PM μπαίνει στο building ως PropertyManager
        Role pmRole = roleRepository.findByName("PropertyManager")
                .orElseThrow(() -> new RuntimeException("Role 'PropertyManager' not found"));

        BuildingMember membership = BuildingMember.builder()
                .building(savedBuilding)
                .user(currentUser)
                .role(pmRole)
                .status(BuildingMemberStatus.JOINED)
                .build();

        buildingMemberRepository.save(membership);

        return savedBuilding.getId();
    }




    private String generateBuildingCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public BuildingResponse findById(Integer buildingId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πρόσβαση σε αυτή την πολυκατοικία");
        }

        return buildingRepository.findById(buildingId)
                .map(buildingMapper::toBuildingResponse)
                .orElseThrow(() -> new EntityNotFoundException("No Building Found with the ID: " + buildingId));
    }

    public PageResponse<BuildingResponse> findAllBuildings(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Building> buildings = buildingRepository.findAllDisplayableBuildings(pageable, user.getId());
        List<BuildingResponse> buildingResponses = buildings.stream()
                .map(buildingMapper::toBuildingResponse)
                .toList();
        return new PageResponse<>(
                buildingResponses,
                buildings.getNumber(),
                buildings.getSize(),
                buildings.getTotalElements(),
                buildings.getTotalPages(),
                buildings.isFirst(),
                buildings.isLast()
        );
    }

    public PageResponse<BuildingResponse> findAllBuildingsByManager(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Building> buildings = buildingRepository.findAll(BuildingSpecification.withManagerId(user.getId()), pageable);

        List<BuildingResponse> buildingResponses = buildings.stream()
                .map(buildingMapper::toBuildingResponse)
                .toList();
        return new PageResponse<>(
                buildingResponses,
                buildings.getNumber(),
                buildings.getSize(),
                buildings.getTotalElements(),
                buildings.getTotalPages(),
                buildings.isFirst(),
                buildings.isLast()
        );
    }
    public Optional<BuildingResponse> findBuildingOfCurrentUser(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        return buildingRepository.findAllByUserId(user.getId()).stream()
                .findFirst()
                .map(buildingMapper::toBuildingResponse);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE)
    public void deleteBuilding(Integer buildingId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        if (!buildingPermissionService.canFullManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πλήρη δικαιώματα διαχείρισης αυτής της πολυκατοικίας");
        }

        // soft-delete: keep the record but mark it inactive/disabled
        building.setActive(false);
        building.setEnable(false);
        buildingRepository.save(building);
    }

    @Transactional
    public void setBuildingActive(Integer buildingId, boolean active, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        if (!buildingPermissionService.canFullManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πλήρη δικαιώματα διαχείρισης αυτής της πολυκατοικίας");
        }

        building.setActive(active);
        building.setEnable(active);
        buildingRepository.save(building);
    }

    @Transactional
    public void deleteDraftBuilding(Integer buildingId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        //allow only creator (draft rollback)
        if (building.getCreatedBy() == null || !building.getCreatedBy().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed to delete this draft building");
        }

        //if apartments/members exist, delete them first (αν δεν έχεις cascade)
        apartmentRepository.deleteByBuildingId(buildingId);
        buildingMemberRepository.deleteByBuildingId(buildingId);

        buildingRepository.delete(building);
    }

    public ManagerDTO getManagerDTO(Integer buildingId) {
        Building b = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + buildingId));

        if (b.getManager() == null) {
            throw new EntityNotFoundException("Manager not set for building " + buildingId);
        }

        var m = b.getManager();
        String fullName = (m.getFirstName() + " " + m.getLastName()).trim();

        return new ManagerDTO(
                m.getId(),
                fullName,
                m.getEmail(),
                m.getRole() != null ? m.getRole().getName() : null,
                m.getPhoneNumber(),
                m.getAddress1(),
                m.getAddressNumber1(),
                m.getAddress2(),
                m.getAddressNumber2(),
                m.getProfileImageUrl()
        );
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE)
    public BuildingDTO updateBuilding(Integer buildingId, BuildingDTO dto, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα επεξεργασίας αυτής της πολυκατοικίας");
        }

        // Update fields
        building.setName(dto.getName());
        building.setStreet1(dto.getStreet1());
        building.setStNumber1(dto.getStNumber1());
        building.setStreet2(dto.getStreet2());
        building.setStNumber2(dto.getStNumber2());
        building.setCity(dto.getCity());
        building.setRegion(dto.getRegion());
        building.setPostalCode(dto.getPostalCode());
        building.setCountry(dto.getCountry());
        building.setState(dto.getState());
        building.setFloors(dto.getFloors());
        building.setApartmentsNum(dto.getApartmentsNum());
        building.setSqMetersTotal(dto.getSqMetersTotal());
        building.setSqMetersCommonSpaces(dto.getSqMetersCommonSpaces());

        building.setParkingExist(dto.isParkingExist());
        building.setParkingSpacesNum(dto.getParkingSpacesNum());

        building.setUndergroundFloorExist(dto.isUndergroundFloorExist());
        building.setHalfFloorExist(dto.isHalfFloorExist());
        building.setOverTopFloorExist(dto.isOverTopFloorExist());

        building.setStorageExist(dto.isStorageExist());
        building.setStorageNum(dto.getStorageNum());

        building.setHasCentralHeating(dto.isHasCentralHeating());
        if (dto.getHeatingType() != null) {
            building.setHeatingType(HeatingType.valueOf(dto.getHeatingType().toUpperCase()));
        }
        building.setHeatingCapacityLitres(dto.getHeatingCapacityLitres());

        building.setBuildingDescription(dto.getBuildingDescription());

        Building updated = buildingRepository.save(building);
        return buildingMapper.toDTO(updated);
    }

    public List<ManagedBuildingDTO> getManagedBuildings(Integer userId) {
        return buildingRepository.findByManagerId(userId)
                .stream()
                .map(b -> new ManagedBuildingDTO(
                        b.getId(),
                        b.getName(),
                        b.getStreet1(),
                        b.getStNumber1(),
                        b.getStreet2(),
                        b.getStNumber2(),
                        b.getCity(),
                        b.getPostalCode(),
                        new ManagerDTO(
                                b.getManager().getId(),
                                b.getManager().getFullName(),
                                b.getManager().getEmail(),
                                b.getManager().getRole() != null ? b.getManager().getRole().getName() : null,
                                b.getManager().getPhoneNumber(),
                                b.getManager().getAddress1(),
                                b.getManager().getAddressNumber1(),
                                b.getManager().getAddress2(),
                                b.getManager().getAddressNumber2(),
                                b.getManager().getProfileImageUrl()
                        )
                ))
                .toList();
    }
    public List<BuildingDTO> getMyBuildingsDTO(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        var fromManager = buildingRepository.findByManagerId(user.getId());

        var fromPropertyManager = buildingRepository.findByPropertyManager_Id(user.getId());

        var fromApartments = apartmentRepository.findByOwnerOrResident(user, user)
                .stream()
                .map(Apartment::getBuilding)
                .toList();

        var fromPermissions = userBuildingPermissionRepository.findByUserId(user.getId())
                .stream()
                .map(permission -> permission.getBuilding())
                .toList();

        return Stream.of(fromManager, fromPropertyManager, fromApartments, fromPermissions)
                .flatMap(List::stream)
                .distinct()
                .map(buildingMapper::toDTO)
                .toList();
    }

    public List<BuildingResponse> getMyCompanyBuildings(Authentication auth) {
        User user = freshUser(auth);

        if (user.getRole() == null || !"PropertyManager".equals(user.getRole().getName())) {
            throw new AccessDeniedException("Μόνο PropertyManager.");
        }
        if (user.getCompany() == null) {
            return List.of(); // ή throw αν θες
        }

        return buildingRepository.findByCompany_Id(user.getCompany().getId())
                .stream()
                .map(buildingMapper::toBuildingResponse) // έχει και company μέσα
                .toList();
    }

    public List<BuildingDTO> getAllBuildingsForAdmin(User currentUser) {
        String role = currentUser.getRole().getName().trim().toUpperCase().replace(" ", "_");

        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("Μόνο Admin.");
        }

        return buildingRepository.findAll()
                .stream()
                .map(buildingMapper::toDTO)
                .toList();
    }

    public Map<String, String> uploadBuildingImage(Integer buildingId, MultipartFile file, Authentication auth) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = extractImageExtension(originalFilename);

        User user = freshUser(auth);

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα αλλαγής της φωτογραφίας αυτής της πολυκατοικίας");
        }

        String fileName = UUID.randomUUID() + extension;
        Path buildingImageDir = Paths.get("uploads", "buildings", String.valueOf(buildingId), "profile-image");

        try {
            Files.createDirectories(buildingImageDir);

            deleteOldBuildingImage(building);

            Path filePath = buildingImageDir.resolve(fileName);
            log.debug("Saving building image to: {}", filePath.toAbsolutePath());

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/buildings/" + buildingId + "/profile-image/" + fileName;

            building.setProfileImageUrl(imageUrl);
            buildingRepository.save(building);

            return Map.of("imageUrl", imageUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload building image", e);
        }
    }

    private String extractImageExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Το αρχείο δεν έχει έγκυρη επέκταση.");
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Ο τύπος αρχείου ." + extension + " δεν υποστηρίζεται.");
        }

        return "." + extension;
    }

    private void deleteOldBuildingImage(Building building) {
        String currentUrl = building.getProfileImageUrl();
        if (currentUrl == null || currentUrl.isBlank() || !currentUrl.startsWith("/uploads/buildings/")) {
            return;
        }

        String relativePath = currentUrl.substring("/uploads/".length());
        Path root = Paths.get("uploads").toAbsolutePath().normalize();
        Path filePath = root.resolve(relativePath).normalize();

        if (!filePath.startsWith(root)) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Δεν ήταν δυνατή η διαγραφή της παλιάς φωτογραφίας πολυκατοικίας: {}", filePath);
        }
    }

}

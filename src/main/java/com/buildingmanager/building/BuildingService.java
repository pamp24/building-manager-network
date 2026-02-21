package com.buildingmanager.building;


import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final RoleRepository roleRepository;

    private User freshUser(Authentication auth) {
        User principal = (User) auth.getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
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
                        .status("Joined")
                        .build()
        );

        // global role upgrade
        currentUser.setRole(bmRole);
        userRepository.save(currentUser);

        return saved.getId();
    }

    @Transactional
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
                        .status("Joined")
                        .build()
        );

        return saved.getId();
    }


    @Transactional
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
                .status("Joined")
                .build();

        buildingMemberRepository.save(membership);

        return savedBuilding.getId();
    }




    private String generateBuildingCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public BuildingResponse findById(Integer buildingId){

        return buildingRepository.findById(buildingId)
                .map(buildingMapper::toBuildingResponse)
                .orElseThrow(() -> new EntityNotFoundException("No Building Found with the ID: "+ buildingId));
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
    public void deleteBuilding(Integer buildingId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        boolean userIsLinked = buildingMemberRepository
                .findByBuildingId(buildingId)
                .stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()));

        if (!userIsLinked) {
            throw new AccessDeniedException("Not allowed to delete this building");
        }

        buildingRepository.delete(building);
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
    public BuildingDTO updateBuilding(Integer buildingId, BuildingDTO dto, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));

        boolean userIsManager = buildingMemberRepository
                .findByBuildingId(buildingId)
                .stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId())
                        && m.getRole().getName().equals("BuildingManager"));

        if (!userIsManager) {
            throw new AccessDeniedException("Μόνο ο διαχειριστής μπορεί να επεξεργαστεί την πολυκατοικία");
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

        return Stream.of(fromManager, fromPropertyManager, fromApartments)
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

        return buildingRepository.findByCompanyId(user.getCompany().getId())
                .stream()
                .map(buildingMapper::toBuildingResponse) // έχει και company μέσα
                .toList();
    }

}

package com.buildingmanager.building;


import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleName;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;


import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class BuildingService {


    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public Integer save(BuildingRequest request, Authentication connectedUser) {
        User currentUser = (User) connectedUser.getPrincipal();

        // Μετατροπή request -> Building
        Building building = buildingMapper.toBuilding(request);
        building.setBuildingCode(generateBuildingCode());

        Building savedBuilding = buildingRepository.save(building);

        //Φόρτωσε το Role entity από το RoleRepository
        Role managerRole = roleRepository.findByName("BuildingManager")
                .orElseThrow(() -> new RuntimeException("Role 'BuildingManager' not found"));

        //Local membership
        com.buildingmanager.building.BuildingMember membership = com.buildingmanager.building.BuildingMember.builder()
                .building(savedBuilding)
                .user(currentUser)
                .role(managerRole)   // βάζουμε persisted entity
                .status("Joined")
                .build();

        buildingMemberRepository.save(membership);

        //Global role upgrade
        if (!"BuildingManager".equalsIgnoreCase(currentUser.getRole().getName())) {
            currentUser.setRole(managerRole);
            userRepository.save(currentUser);
        }

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
    public ManagerDTO getManagerDTO(Integer buildingId) {
        Building b = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + buildingId));

        if (b.getManager() == null) {
            throw new EntityNotFoundException("Manager not set for building " + buildingId);
        }
        var m = b.getManager();

        String fullName = (m.getFirstName() + " " + m.getLastName()).trim();
        return new ManagerDTO(m.getId(), fullName);
    }

    public List<BuildingResponse> getMyBuildings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // Βρες όλα τα διαμερίσματα που ανήκουν στον χρήστη
        List<Apartment> apartments = apartmentRepository.findByOwnerOrResident(user, user);

        // Από τα διαμερίσματα μάζεψε τις πολυκατοικίες (unique)
        return apartments.stream()
                .map(Apartment::getBuilding)
                .distinct()
                .map(buildingMapper::toBuildingResponse)
                .toList();
    }

}

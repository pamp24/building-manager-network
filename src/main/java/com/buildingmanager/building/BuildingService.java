package com.buildingmanager.building;


import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.data.domain.Pageable;

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
                .role(managerRole)
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

        // Φαντάζομαι ότι το User (manager) entity έχει email, phone, address
        return new ManagerDTO(
                m.getId(),
                fullName,
                m.getEmail(),
                m.getPhoneNumber(),
                m.getAddress1(),
                m.getAddressNumber1(),
                m.getAddress2(),
                m.getAddressNumber2()
        );
    }

    public List<BuildingResponse> getMyBuildings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        var fromManager = buildingRepository.findByManagerId(user.getId());

        var fromApartments = apartmentRepository.findByOwnerOrResident(user, user)
                .stream()
                .map(Apartment::getBuilding)
                .toList();

        return Stream.concat(fromApartments.stream(), fromManager.stream())
                .distinct()
                .map(buildingMapper::toBuildingResponse)
                .toList();
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
                                b.getManager().getPhoneNumber(),
                                b.getManager().getAddress1(),
                                b.getManager().getAddressNumber1(),
                                b.getManager().getAddress2(),
                                b.getManager().getAddressNumber2()
                        )
                ))
                .toList();
    }


}

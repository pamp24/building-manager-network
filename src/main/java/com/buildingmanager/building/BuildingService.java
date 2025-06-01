package com.buildingmanager.building;


import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.util.List;


@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;
    public Integer save(BuildingRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Building building = buildingMapper.toBuilding(request);
        building.setUsers(List.of(user));
        return buildingRepository.save(building).getId();
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

}

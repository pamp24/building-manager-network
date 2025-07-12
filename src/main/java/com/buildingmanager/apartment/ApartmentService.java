package com.buildingmanager.apartment;

import com.buildingmanager.exceptions.OperationNotPermittedException;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApartmentService {
    private final BuildingRepository buildingRepository;
    private final ApartmentMapper apartmentMapper;
    private final ApartmentRepository apartmentRepository;

    public Object save(ApartmentRequest request, Authentication connectedUser) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(()-> new EntityNotFoundException("No Building found with ID:: " + request.buildingId()));
        if (building.isEnable() || !building.isActive()) {
            throw new OperationNotPermittedException("You cannot make any changes to this apartment");
        }
        User userEntity = ((User) connectedUser.getPrincipal());
        if (Objects.equals(building.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot make any changes to this apartment since you are not the owner");
        }
        Apartment apartment = apartmentMapper.toApartment(request);
        return apartmentRepository.save(apartment).getId();
    }

    @Transactional
    public PageResponse<ApartmentResponse> findAllApartmentsByBuilding(Integer buildingId, int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size);
        User userEntity = ((User) connectedUser.getPrincipal());
        Page<Apartment> apartments = apartmentRepository.findAllByBuildingId(buildingId, pageable);
        List<ApartmentResponse> apartmentResponses = apartments.stream()
                .map(f -> (ApartmentResponse) apartmentMapper.toApartmentResponse(f, userEntity.getId()))
                .toList();
        return new PageResponse<>(
                apartmentResponses,
                apartments.getNumber(),
                apartments.getSize(),
                apartments.getTotalElements(),
                apartments.getTotalPages(),
                apartments.isFirst(),
                apartments.isLast()
        );
    }
}

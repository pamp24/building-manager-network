package com.buildingmanager.apartment;

import com.buildingmanager.exceptions.OperationNotPermittedException;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
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
    private final UserRepository userRepository;

    public Object save(ApartmentRequest request, Authentication connectedUser) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(()-> new EntityNotFoundException("No Building found with ID:: " + request.buildingId()));

        User userEntity = ((User) connectedUser.getPrincipal());
        Apartment apartment = apartmentMapper.toApartment(request);
        // Εύρεση και ορισμός resident
        if (request.residentId() != null) {
            User resident = userRepository.findById(request.residentId())
                    .orElseThrow(() -> new EntityNotFoundException("Resident not found"));
            apartment.setResident(resident);
        }
        // Εύρεση και ορισμός owner
        if (request.ownerId() != null) {
            User owner = userRepository.findById(request.ownerId())
                    .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
            apartment.setOwner(owner);
        }
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

    @Transactional
    public void saveAll(List<ApartmentRequest> requests, Authentication connectedUser) {
        User userEntity = (User) connectedUser.getPrincipal();

        for (ApartmentRequest request : requests) {
            Building building = buildingRepository.findById(request.buildingId())
                    .orElseThrow(() -> new EntityNotFoundException("No Building found with ID:: " + request.buildingId()));


            if (!Objects.equals(building.getCreatedBy(), userEntity.getId())) {
                throw new OperationNotPermittedException("You are not the owner of this building.");
            }

            Apartment apartment = apartmentMapper.toApartment(request);
            apartmentRepository.save(apartment);
        }
    }

    public Apartment findByUser(Integer userId) {
        return apartmentRepository.findByResident_Id(userId)
                .or(() -> apartmentRepository.findByOwner_Id(userId))
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found for user id: " + userId));
    }

    public List<ApartmentResponse> getApartmentsInSameBuilding(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        // 1. Βρες το διαμέρισμα του χρήστη
        Apartment userApartment = apartmentRepository.findByResident_Id(user.getId())  // ή findByOwnerId αν έχεις μόνο owners
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε διαμέρισμα για τον χρήστη."));
        // 2. Πάρε το buildingId
        Integer buildingId = userApartment.getBuilding().getId();
        // 3. Φέρε όλα τα διαμερίσματα με το ίδιο buildingId
        Pageable pageable = PageRequest.of(0, 100); // ή από controller
        Page<Apartment> apartmentsPage = apartmentRepository.findAllByBuildingId(buildingId, pageable);
        List<Apartment> apartments = apartmentsPage.getContent();
        // 4. Κάνε map σε response
        return apartments.stream()
                .map(apartment -> apartmentMapper.toApartmentResponse(apartment, user.getId()))
                .toList();
    }




}

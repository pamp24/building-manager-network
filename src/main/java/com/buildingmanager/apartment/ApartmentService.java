package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {
    private final BuildingRepository buildingRepository;
    private final ApartmentMapper apartmentMapper;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final BuildingPermissionService buildingPermissionService;

    public Object save(ApartmentRequest request, Authentication connectedUser) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new EntityNotFoundException("No Building found with ID:: " + request.buildingId()));
        User userEntity = (User) connectedUser.getPrincipal();

        if (!buildingPermissionService.canManageBuilding(userEntity, request.buildingId())) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα δημιουργίας διαμερίσματος σε αυτή την πολυκατοικία");
        }

        Apartment apartment = apartmentMapper.toApartment(request);
        // Αν το διαμέρισμα είναι "Διαμέρισμα Διαχειριστή", θέλουμε να ορίσουμε τον ownerId ως τον χρήστη που το δημιουργεί
        if (request.isManagerHouse()) {
            apartment.setOwner(userEntity); // Το ownerId γίνεται το userId του συνδεδεμένου χρήστη
        }
        // Εύρεση και ορισμός resident
        if (request.residentId() != null) {
            User resident = userRepository.findById(request.residentId())
                    .orElseThrow(() -> new EntityNotFoundException("Resident not found"));
            apartment.setResident(resident);
        }
        // Εύρεση και ορισμός owner αν δεν είναι διαμέρισμα διαχειριστή
        if (!request.isManagerHouse() && request.ownerId() != null) {
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

        if (!buildingPermissionService.canViewBuilding(userEntity, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πρόσβαση στα διαμερίσματα αυτής της πολυκατοικίας");
        }

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


            if (!buildingPermissionService.canManageBuilding(userEntity, request.buildingId())) {
                throw new AccessDeniedException("Δεν έχεις δικαίωμα δημιουργίας διαμερισμάτων σε αυτή την πολυκατοικία");
            }

            Apartment apartment = apartmentMapper.toApartment(request);
            apartmentRepository.save(apartment);
        }
    }

    public List<Apartment> findByUser(Integer userId) {
        List<Apartment> asResident = apartmentRepository.findByResident_Id(userId);
        List<Apartment> asOwner = apartmentRepository.findByOwner_Id(userId);

        List<Apartment> result = new ArrayList<>();
        result.addAll(asResident);
        result.addAll(asOwner);
        return result;
    }

    public List<ApartmentResponse> getApartmentsInSameBuilding(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        // Φέρε όλα τα apartments που συνδέονται με τον user (σαν resident ή owner)
        List<Apartment> userApartments = new ArrayList<>();
        userApartments.addAll(apartmentRepository.findByResident_Id(user.getId()));
        userApartments.addAll(apartmentRepository.findByOwner_Id(user.getId()));

        if (userApartments.isEmpty()) {
            return Collections.emptyList();
        }

        // Παίρνουμε το πρώτο apartment (ή μπορείς να κάνεις loop αν θες πολλά buildings)
        Apartment userApartment = userApartments.get(0);
        Integer buildingId = userApartment.getBuilding().getId();

        Pageable pageable = PageRequest.of(0, 100);
        Page<Apartment> apartmentsPage = apartmentRepository.findAllByBuildingId(buildingId, pageable);

        return apartmentsPage.getContent().stream()
                .map(apartment -> apartmentMapper.toApartmentResponse(apartment, user.getId()))
                .toList();
    }

    public List<ApartmentResponse> getMyApartments(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Apartment> apartments = apartmentRepository
                .findByOwnerOrResident(user, user);

        return apartments.stream()
                .map(apartmentMapper::toApartmentResponse)
                .toList();
    }

    public List<ApartmentResponse> getAvailableApartments(Integer buildingId, String role, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πρόσβαση σε αυτή την πολυκατοικία");
        }

        return apartmentRepository.findAvailableApartmentsForRole(buildingId, role)
                .stream()
                .map(apartmentMapper::toApartmentResponse)
                .toList();
    }
    @Transactional
    public ApartmentResponse updateApartment(
            Integer apartmentId,
            ApartmentDTO dto,
            Authentication connectedUser
    ) {
        User user = (User) connectedUser.getPrincipal();

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Apartment not found with id " + apartmentId
                        )
                );

        Integer buildingId = apartment.getBuilding().getId();

        boolean isOwner =
                apartment.getOwner() != null &&
                        apartment.getOwner().getId().equals(user.getId());

        boolean isResident =
                apartment.getResident() != null &&
                        apartment.getResident().getId().equals(user.getId());

        boolean canManageBuilding =
                buildingPermissionService.canManageBuilding(
                        user,
                        buildingId
                );

        if (!isOwner && !isResident && !canManageBuilding) {
            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα να επεξεργαστείτε αυτό το διαμέρισμα"
            );
        }

        apartment.setNumber(dto.getNumber());
        apartment.setSqMetersApart(dto.getSqMetersApart());
        apartment.setFloor(dto.getFloor());

        apartment.setParkingSpace(
                dto.getParkingSpace() != null
                        ? dto.getParkingSpace()
                        : false
        );

        apartment.setParkingSlot(
                Boolean.TRUE.equals(dto.getParkingSpace())
                        ? dto.getParkingSlot()
                        : null
        );

        apartment.setIsRented(
                dto.getIsRented() != null
                        ? dto.getIsRented()
                        : false
        );

        apartment.setApStorageExist(
                dto.getApStorageExist() != null
                        ? dto.getApStorageExist()
                        : false
        );

        apartment.setStorageSlot(
                Boolean.TRUE.equals(dto.getApStorageExist())
                        ? dto.getStorageSlot()
                        : null
        );

        apartment.setCommonPercent(dto.getCommonPercent());
        apartment.setElevatorPercent(dto.getElevatorPercent());
        apartment.setHeatingPercent(dto.getHeatingPercent());
        apartment.setApDescription(dto.getApDescription());

        /*
         * Εφόσον ο πραγματικός user συνδέεται αργότερα,
         * ενημερώνουμε και τα προσωρινά πεδία του apartment.
         */
        apartment.setOwnerFirstName(dto.getOwnerFirstName());
        apartment.setOwnerLastName(dto.getOwnerLastName());

        apartment.setResidentFirstName(
                Boolean.TRUE.equals(dto.getIsRented())
                        ? dto.getResidentFirstName()
                        : null
        );

        apartment.setResidentLastName(
                Boolean.TRUE.equals(dto.getIsRented())
                        ? dto.getResidentLastName()
                        : null
        );

        Apartment saved = apartmentRepository.save(apartment);

        return apartmentMapper.toApartmentResponse(saved);
    }

    @Transactional
    public void deleteApartment(
            Integer apartmentId,
            Authentication connectedUser
    ) {
        User user = (User) connectedUser.getPrincipal();

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Apartment not found with id " + apartmentId
                        )
                );

        Integer buildingId =
                apartment.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα διαγραφής αυτού του διαμερίσματος"
            );
        }

        apartment.setActive(false);
        apartment.setEnable(false);

        apartmentRepository.save(apartment);
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> getApartmentsByBuildingList(
            Integer buildingId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException(
                    "Δεν έχεις πρόσβαση στα διαμερίσματα αυτής της πολυκατοικίας"
            );
        }
        return apartmentRepository
                .findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(
                        buildingId
                )
                .stream()
                .map(apartment ->
                        apartmentMapper.toApartmentResponse(
                                apartment,
                                user.getId()
                        )
                )
                .toList();
    }


}

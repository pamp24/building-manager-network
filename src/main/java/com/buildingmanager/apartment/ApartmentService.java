package com.buildingmanager.apartment;

import com.buildingmanager.exceptions.OperationNotPermittedException;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.invite.InviteRepository;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ApartmentService {
    private final BuildingRepository buildingRepository;
    private final ApartmentMapper apartmentMapper;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;

    public Object save(ApartmentRequest request, Authentication connectedUser) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new EntityNotFoundException("No Building found with ID:: " + request.buildingId()));
        User userEntity = (User) connectedUser.getPrincipal();
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

    public ApartmentResponse updateMyApartment(ApartmentRequest request, Authentication auth) {
        User user = (User) auth.getPrincipal();

        Apartment apartment = apartmentRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found"));

        // Έλεγχος αν είναι owner ή resident
        if (!Objects.equals(apartment.getOwner().getId(), user.getId()) &&
                (apartment.getResident() == null || !Objects.equals(apartment.getResident().getId(), user.getId()))) {
            throw new SecurityException("Δεν έχεις δικαίωμα να ενημερώσεις αυτό το διαμέρισμα");
        }
        // ενημέρωση πεδίων
        apartment.setOwnerFirstName(request.ownerFirstName());
        apartment.setOwnerLastName(request.ownerLastName());
        apartment.setNumber(request.number());
        apartment.setSqMetersApart(request.sqMetersApart());
        apartment.setFloor(request.floor());
        apartment.setParkingSpace(request.parkingSpace());
        apartment.setParkingSlot(request.parkingSlot());
        apartment.setApStorageExist(request.apStorageExist());
        apartment.setStorageSlot(request.storageSlot());
        apartment.setIsManagerHouse(request.isManagerHouse());
        apartment.setCommonPercent(request.commonPercent());
        apartment.setElevatorPercent(request.elevatorPercent());
        apartment.setHeatingPercent(request.heatingPercent());
        apartment.setApDescription(request.apDescription());

        return apartmentMapper.toApartmentResponse(apartmentRepository.save(apartment), user.getId());
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

    public List<ApartmentResponse> getAvailableApartments(Integer buildingId, String role) {
        return apartmentRepository.findAvailableApartmentsForRole(buildingId, role)
                .stream()
                .map(apartmentMapper::toApartmentResponse)
                .toList();
    }
    @Transactional
    public ApartmentResponse updateMyApartment(ApartmentDTO dto, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Apartment apartment = apartmentRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found with id " + dto.getId()));

        // Έλεγχος: μόνο ο ιδιοκτήτης ή ο ένοικος μπορεί να ενημερώσει
        boolean isOwner = apartment.getOwner() != null && apartment.getOwner().getId().equals(user.getId());
        boolean isResident = apartment.getResident() != null && apartment.getResident().getId().equals(user.getId());

        if (!isOwner && !isResident) {
            throw new AccessDeniedException("Δεν έχετε δικαίωμα να επεξεργαστείτε αυτό το διαμέρισμα");
        }

        // Ενημέρωση πεδίων
        apartment.setNumber(dto.getNumber());
        apartment.setSqMetersApart(dto.getSqMetersApart());
        apartment.setFloor(dto.getFloor());
        apartment.setParkingSpace(
                dto.getParkingSpace() != null ? dto.getParkingSpace() : false
        );
        apartment.setIsRented(dto.getIsRented());
        apartment.setParkingSlot(dto.getParkingSlot());
        apartment.setApStorageExist(
                dto.getApStorageExist() != null ? dto.getApStorageExist() : false
        );
        apartment.setStorageSlot(dto.getStorageSlot());
        apartment.setIsManagerHouse(
                dto.getIsManagerHouse() != null ? dto.getIsManagerHouse() : false
        );
        apartment.setCommonPercent(dto.getCommonPercent());
        apartment.setElevatorPercent(dto.getElevatorPercent());
        apartment.setHeatingPercent(dto.getHeatingPercent());
        apartment.setApDescription(dto.getApDescription());

        // Ενημέρωση στοιχείων resident (αν έχει)
        if (dto.getResidentFirstName() != null && apartment.getResident() != null) {
            apartment.getResident().setFirstName(dto.getResidentFirstName());
        }
        if (dto.getResidentLastName() != null && apartment.getResident() != null) {
            apartment.getResident().setLastName(dto.getResidentLastName());
        }

        // Ενημέρωση στοιχείων owner (αν έχει)
        if (dto.getOwnerFirstName() != null && apartment.getOwner() != null) {
            apartment.getOwner().setFirstName(dto.getOwnerFirstName());
        }
        if (dto.getOwnerLastName() != null && apartment.getOwner() != null) {
            apartment.getOwner().setLastName(dto.getOwnerLastName());
        }

        Apartment saved = apartmentRepository.save(apartment);

        return apartmentMapper.toApartmentResponse(saved);
    }



}

package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.common.PageResponse;
import com.buildingmanager.exceptions.BusinessValidationException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Transactional
    public Object save(
            ApartmentRequest request,
            Authentication connectedUser
    ) {

        Building building = buildingRepository
                .findById(request.buildingId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "No Building found with ID:: "
                                        + request.buildingId()
                        )
                );

        User userEntity =
                (User) connectedUser.getPrincipal();

        if (!buildingPermissionService.canManageBuilding(
                userEntity,
                request.buildingId()
        )) {
            throw new AccessDeniedException(
                    "Δεν έχεις δικαίωμα δημιουργίας διαμερίσματος "
                            + "σε αυτή την πολυκατοικία"
            );
        }

        validateApartmentData(
                building.getId(),
                null,
                false,
                false,

                request.ownerFirstName(),
                request.ownerLastName(),
                request.number(),
                request.floor(),
                request.sqMetersApart(),

                request.commonPercent(),
                request.elevatorPercent(),
                request.heatingPercent(),

                request.isRented(),
                request.residentFirstName(),
                request.residentLastName(),

                request.parkingSpace(),
                request.parkingSlot(),

                request.apStorageExist(),
                request.storageSlot(),

                building.getParkingSpacesNum(),
                building.getStorageNum()
        );

        Apartment apartment =
                apartmentMapper.toApartment(request);

        if (request.isManagerHouse()) {
            apartment.setOwner(userEntity);
        }

        if (request.residentId() != null) {
            User resident = userRepository
                    .findById(request.residentId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Resident not found"
                            )
                    );

            apartment.setResident(resident);
        }

        if (!request.isManagerHouse()
                && request.ownerId() != null) {

            User owner = userRepository
                    .findById(request.ownerId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Owner not found"
                            )
                    );

            apartment.setOwner(owner);
        }

        Apartment savedApartment =
                apartmentRepository.save(apartment);

        return savedApartment.getId();
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
    public void saveAll(
            List<ApartmentRequest> requests,
            Authentication connectedUser
    ) {

        User userEntity =
                (User) connectedUser.getPrincipal();

        for (ApartmentRequest request : requests) {

            Building building = buildingRepository
                    .findById(request.buildingId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "No Building found with ID:: "
                                            + request.buildingId()
                            )
                    );

            if (!buildingPermissionService.canManageBuilding(
                    userEntity,
                    request.buildingId()
            )) {
                throw new AccessDeniedException(
                        "Δεν έχεις δικαίωμα δημιουργίας διαμερισμάτων "
                                + "σε αυτή την πολυκατοικία"
                );
            }

            validateApartmentData(
                    building.getId(),
                    null,
                    false,
                    false,

                    request.ownerFirstName(),
                    request.ownerLastName(),
                    request.number(),
                    request.floor(),
                    request.sqMetersApart(),

                    request.commonPercent(),
                    request.elevatorPercent(),
                    request.heatingPercent(),

                    request.isRented(),
                    request.residentFirstName(),
                    request.residentLastName(),

                    request.parkingSpace(),
                    request.parkingSlot(),

                    request.apStorageExist(),
                    request.storageSlot(),

                    building.getParkingSpacesNum(),
                    building.getStorageNum()
            );

            Apartment apartment =
                    apartmentMapper.toApartment(request);

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
            ApartmentUpdateRequest request,
            Authentication connectedUser
    ) {
        User user = (User) connectedUser.getPrincipal();

        Apartment apartment = apartmentRepository
                .findById(apartmentId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Apartment not found with id " + apartmentId
                        )
                );

        Integer buildingId =
                apartment.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(
                user,
                buildingId
        )) {
            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα επεξεργασίας αυτού του διαμερίσματος"
            );
        }

        validateApartmentData(
                buildingId,
                apartment.getId(),
                apartment.getParkingSpace(),
                apartment.getApStorageExist(),

                request.getOwnerFirstName(),
                request.getOwnerLastName(),
                request.getNumber(),
                request.getFloor(),
                request.getSqMetersApart(),

                request.getCommonPercent(),
                request.getElevatorPercent(),
                request.getHeatingPercent(),

                request.getRented(),
                request.getResidentFirstName(),
                request.getResidentLastName(),

                request.getParkingSpace(),
                request.getParkingSlot(),

                request.getStorageExist(),
                request.getStorageSlot(),

                apartment.getBuilding().getParkingSpacesNum(),
                apartment.getBuilding().getStorageNum()
        );


        apartment.setOwnerFirstName(
                normalize(request.getOwnerFirstName())
        );

        apartment.setOwnerLastName(
                normalize(request.getOwnerLastName())
        );

        apartment.setNumber(
                normalize(request.getNumber())
        );

        apartment.setFloor(
                normalize(request.getFloor())
        );

        apartment.setSqMetersApart(
                normalize(request.getSqMetersApart())
        );

        apartment.setParkingSpace(
                Boolean.TRUE.equals(
                        request.getParkingSpace()
                )
        );

        apartment.setParkingSlot(
                Boolean.TRUE.equals(request.getParkingSpace())
                        ? normalize(request.getParkingSlot())
                        : null
        );

        apartment.setIsRented(
                Boolean.TRUE.equals(request.getRented())
        );

        if (Boolean.TRUE.equals(request.getRented())) {
            apartment.setResidentFirstName(
                    normalize(request.getResidentFirstName())
            );

            apartment.setResidentLastName(
                    normalize(request.getResidentLastName())
            );
        } else {
            apartment.setResidentFirstName(null);
            apartment.setResidentLastName(null);
        }

        apartment.setCommonPercent(
                request.getCommonPercent()
        );

        apartment.setElevatorPercent(
                request.getElevatorPercent()
        );

        apartment.setHeatingPercent(
                request.getHeatingPercent()
        );

        apartment.setApStorageExist(
                Boolean.TRUE.equals(
                        request.getStorageExist()
                )
        );

        apartment.setStorageSlot(
                Boolean.TRUE.equals(request.getStorageExist())
                        ? normalize(request.getStorageSlot())
                        : null
        );

        apartment.setApDescription(
                normalize(request.getDescription())
        );

        Apartment savedApartment =
                apartmentRepository.save(apartment);

        return apartmentMapper.toApartmentResponse(
                savedApartment,
                user.getId()
        );
    }

    @Transactional
    public ApartmentResponse updateMyApartment(
            MyApartmentUpdateRequest request,
            Authentication connectedUser
    ) {
        User user = (User) connectedUser.getPrincipal();

        if (request.getId() == null) {
            throw new BusinessValidationException(
                    "Δεν προσδιορίστηκε το διαμέρισμα."
            );
        }

        Apartment apartment = apartmentRepository
                .findById(request.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Apartment not found with id " + request.getId()
                        )
                );

        boolean isOwner = apartment.getOwner() != null
                && apartment.getOwner().getId().equals(user.getId());
        boolean isResident = apartment.getResident() != null
                && apartment.getResident().getId().equals(user.getId());

        if (!isOwner && !isResident) {
            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα επεξεργασίας αυτού του διαμερίσματος"
            );
        }

        apartment.setOwnerFirstName(
                normalize(request.getOwnerFirstName())
        );

        apartment.setOwnerLastName(
                normalize(request.getOwnerLastName())
        );

        apartment.setResidentFirstName(
                normalize(request.getResidentFirstName())
        );

        apartment.setResidentLastName(
                normalize(request.getResidentLastName())
        );

        apartment.setApDescription(
                normalize(request.getDescription())
        );

        Apartment savedApartment =
                apartmentRepository.save(apartment);

        return apartmentMapper.toApartmentResponse(
                savedApartment,
                user.getId()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private void validateRequiredFields(
            String ownerFirstName,
            String ownerLastName,
            String number,
            String floor,
            String squareMeters
    ) {

        if (normalize(ownerFirstName) == null) {
            throw new BusinessValidationException(
                    "Το όνομα ιδιοκτήτη είναι υποχρεωτικό."
            );
        }

        if (normalize(ownerLastName) == null) {
            throw new BusinessValidationException(
                    "Το επώνυμο ιδιοκτήτη είναι υποχρεωτικό."
            );
        }

        if (normalize(number) == null) {
            throw new BusinessValidationException(
                    "Ο αριθμός διαμερίσματος είναι υποχρεωτικός."
            );
        }

        if (normalize(floor) == null) {
            throw new BusinessValidationException(
                    "Ο όροφος είναι υποχρεωτικός."
            );
        }

        if (normalize(squareMeters) == null) {
            throw new BusinessValidationException(
                    "Τα τετραγωνικά μέτρα είναι υποχρεωτικά."
            );
        }
    }


    private void validateDuplicateApartment(
            Integer buildingId,
            String floor,
            String number,
            Integer apartmentId
    ) {

        boolean exists;

        if (apartmentId == null) {
            exists = apartmentRepository
                    .existsByBuilding_IdAndFloorIgnoreCaseAndNumberIgnoreCaseAndActiveTrue(
                            buildingId,
                            normalize(floor),
                            normalize(number)
                    );
        } else {
            exists = apartmentRepository
                    .existsByBuilding_IdAndFloorIgnoreCaseAndNumberIgnoreCaseAndIdNotAndActiveTrue(
                            buildingId,
                            normalize(floor),
                            normalize(number),
                            apartmentId
                    );
        }

        if (exists) {
            throw new BusinessValidationException(
                    "Υπάρχει ήδη διαμέρισμα με τον ίδιο αριθμό στον συγκεκριμένο όροφο."
            );
        }
    }

    private void validateParking(
            Integer buildingId,
            Integer apartmentId,
            Boolean hadParking,
            Boolean wantsParking,
            String parkingSlot,
            Integer totalParkingSpaces
    ) {

        if (!Boolean.TRUE.equals(wantsParking)) {
            return;
        }

        String normalizedParkingSlot =
                normalize(parkingSlot);

        if (normalizedParkingSlot == null) {
            throw new BusinessValidationException(
                    "Η θέση parking είναι υποχρεωτική."
            );
        }

        boolean parkingExists;

        if (apartmentId == null) {
            parkingExists = apartmentRepository
                    .existsByBuilding_IdAndParkingSlotIgnoreCaseAndActiveTrue(
                            buildingId,
                            normalizedParkingSlot
                    );
        } else {
            parkingExists = apartmentRepository
                    .existsByBuilding_IdAndParkingSlotIgnoreCaseAndIdNotAndActiveTrue(
                            buildingId,
                            normalizedParkingSlot,
                            apartmentId
                    );
        }

        if (parkingExists) {
            throw new BusinessValidationException(
                    "Η θέση parking χρησιμοποιείται ήδη."
            );
        }

        if (!Boolean.TRUE.equals(hadParking)) {

            long usedParkingSpaces =
                    apartmentRepository
                            .countByBuilding_IdAndParkingSpaceTrueAndActiveTrue(
                                    buildingId
                            );

            if (totalParkingSpaces != null &&
                    usedParkingSpaces >= totalParkingSpaces) {

                throw new BusinessValidationException(
                        "Δεν υπάρχουν διαθέσιμες θέσεις parking."
                );
            }
        }
    }

    private void validateStorage(
            Integer buildingId,
            Integer apartmentId,
            Boolean hadStorage,
            Boolean wantsStorage,
            String storageSlot,
            Integer totalStorageSpaces
    ) {

        if (!Boolean.TRUE.equals(wantsStorage)) {
            return;
        }

        String normalizedStorageSlot =
                normalize(storageSlot);

        if (normalizedStorageSlot == null) {
            throw new BusinessValidationException(
                    "Η θέση αποθήκης είναι υποχρεωτική."
            );
        }

        boolean storageExists;

        if (apartmentId == null) {
            storageExists = apartmentRepository
                    .existsByBuilding_IdAndStorageSlotIgnoreCaseAndActiveTrue(
                            buildingId,
                            normalizedStorageSlot
                    );
        } else {
            storageExists = apartmentRepository
                    .existsByBuilding_IdAndStorageSlotIgnoreCaseAndIdNotAndActiveTrue(
                            buildingId,
                            normalizedStorageSlot,
                            apartmentId
                    );
        }

        if (storageExists) {
            throw new BusinessValidationException(
                    "Η θέση αποθήκης χρησιμοποιείται ήδη."
            );
        }

        if (!Boolean.TRUE.equals(hadStorage)) {

            long usedStorageSpaces =
                    apartmentRepository
                            .countByBuilding_IdAndApStorageExistTrueAndActiveTrue(
                                    buildingId
                            );

            if (totalStorageSpaces != null &&
                    usedStorageSpaces >= totalStorageSpaces) {

                throw new BusinessValidationException(
                        "Δεν υπάρχουν διαθέσιμες αποθήκες."
                );
            }
        }
    }

    private void validateSquareMeters(
            String squareMetersValue
    ) {

        String normalizedSquareMeters =
                normalize(squareMetersValue);

        if (normalizedSquareMeters == null) {
            throw new BusinessValidationException(
                    "Τα τετραγωνικά μέτρα είναι υποχρεωτικά."
            );
        }

        try {
            double squareMeters =
                    Double.parseDouble(
                            normalizedSquareMeters.replace(",", ".")
                    );

            if (!Double.isFinite(squareMeters)) {
                throw new BusinessValidationException(
                        "Τα τετραγωνικά δεν είναι έγκυρος αριθμός."
                );
            }

            if (squareMeters <= 0) {
                throw new BusinessValidationException(
                        "Τα τετραγωνικά πρέπει να είναι μεγαλύτερα από το μηδέν."
                );
            }

        } catch (NumberFormatException exception) {
            throw new BusinessValidationException(
                    "Τα τετραγωνικά δεν είναι έγκυρος αριθμός."
            );
        }
    }

    private void validatePercentages(
            Double commonPercent,
            Double elevatorPercent,
            Double heatingPercent
    ) {

        validatePercentage(
                commonPercent,
                "Κοινόχρηστα"
        );

        validatePercentage(
                elevatorPercent,
                "Ασανσέρ"
        );

        validatePercentage(
                heatingPercent,
                "Θέρμανση"
        );
    }

    private void validatePercentage(
            Double value,
            String field
    ) {

        if (value == null) {
            throw new BusinessValidationException(
                    field + " είναι υποχρεωτικό."
            );
        }

        if (value < 0) {
            throw new BusinessValidationException(
                    field + " δεν μπορεί να είναι αρνητικό."
            );
        }

        if (value > 1000) {
            throw new BusinessValidationException(
                    field + " δεν μπορεί να είναι μεγαλύτερο από 1000."
            );
        }

    }

    private void validateResident(
            Boolean rented,
            String residentFirstName,
            String residentLastName
    ) {

        if (!Boolean.TRUE.equals(rented)) {
            return;
        }

        if (normalize(residentFirstName) == null) {
            throw new BusinessValidationException(
                    "Το όνομα ενοικιαστή είναι υποχρεωτικό."
            );
        }

        if (normalize(residentLastName) == null) {
            throw new BusinessValidationException(
                    "Το επώνυμο ενοικιαστή είναι υποχρεωτικό."
            );
        }
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

    @Transactional
    public BigDecimal getCommonPercentSum(Integer buildingId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException(
                    "Δεν έχεις πρόσβαση στα διαμερίσματα αυτής της πολυκατοικίας"
            );
        }
        return apartmentRepository
                .findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(buildingId)
                .stream()
                .map(a -> a.getCommonPercent() == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(a.getCommonPercent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void redistributeCommonPercent(
            Integer buildingId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        if (!buildingPermissionService.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException(
                    "Δεν έχεις δικαίωμα αλλαγής των χιλιοστών αυτής της πολυκατοικίας"
            );
        }

        List<Apartment> apartments = apartmentRepository
                .findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(buildingId);

        if (apartments.isEmpty()) {
            throw new BusinessValidationException(
                    "Δεν υπάρχουν διαμερίσματα σε αυτή την πολυκατοικία."
            );
        }

        int size = apartments.size();

        BigDecimal currentSum = apartments.stream()
                .map(a -> a.getCommonPercent() == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(a.getCommonPercent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainder = BigDecimal.valueOf(1000).subtract(currentSum);

        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal base = remainder.divide(
                BigDecimal.valueOf(size),
                2,
                RoundingMode.DOWN
        );

        BigDecimal baseTotal = base.multiply(BigDecimal.valueOf(size));
        BigDecimal leftover = remainder.subtract(baseTotal);
        int extraCents = leftover.movePointRight(2).intValue();

        for (int i = 0; i < size; i++) {
            Apartment apartment = apartments.get(i);

            double current = apartment.getCommonPercent() == null
                    ? 0.0
                    : apartment.getCommonPercent();

            BigDecimal newValue = BigDecimal.valueOf(current).add(base);

            if (i < extraCents) {
                newValue = newValue.add(BigDecimal.valueOf(0.01));
            }

            apartment.setCommonPercent(
                    newValue.setScale(2, RoundingMode.HALF_UP).doubleValue()
            );

            apartmentRepository.save(apartment);
        }
    }

    private void validateApartmentData(
            Integer buildingId,
            Integer apartmentId,
            Boolean hadParking,
            Boolean hadStorage,
            String ownerFirstName,
            String ownerLastName,
            String number,
            String floor,
            String squareMeters,
            Double commonPercent,
            Double elevatorPercent,
            Double heatingPercent,
            Boolean rented,
            String residentFirstName,
            String residentLastName,
            Boolean parkingSpace,
            String parkingSlot,
            Boolean storageExist,
            String storageSlot,
            Integer totalParkingSpaces,
            Integer totalStorageSpaces
    ) {

        validateRequiredFields(
                ownerFirstName,
                ownerLastName,
                number,
                floor,
                squareMeters
        );

        validateSquareMeters(squareMeters);

        validatePercentages(
                commonPercent,
                elevatorPercent,
                heatingPercent
        );

        validateResident(
                rented,
                residentFirstName,
                residentLastName
        );

        validateDuplicateApartment(
                buildingId,
                floor,
                number,
                apartmentId
        );

        validateParking(
                buildingId,
                apartmentId,
                hadParking,
                parkingSpace,
                parkingSlot,
                totalParkingSpaces
        );

        validateStorage(
                buildingId,
                apartmentId,
                hadStorage,
                storageExist,
                storageSlot,
                totalStorageSpaces
        );
    }


}

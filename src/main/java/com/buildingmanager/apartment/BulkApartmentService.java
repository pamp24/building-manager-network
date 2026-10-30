package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.exceptions.BusinessValidationException;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BulkApartmentService {
    private final BuildingRepository buildings;
    private final ApartmentRepository apartments;
    private final ApartmentMapper mapper;
    private final ApartmentService validation;
    private final BuildingPermissionService permissions;
    private final Validator validator;

    @Transactional
    public List<Integer> importApartments(Integer buildingId, List<ApartmentRequest> requests, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User user)
                || !permissions.canManageBuilding(user, buildingId)) {
            throw new AccessDeniedException("Not authorized to manage this building");
        }
        Building building = buildings.findByIdForUpdate(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + buildingId));
        List<BulkApartmentValidationException.Error> errors = new ArrayList<>();
        if (requests == null || requests.isEmpty() || requests.size() > 500) {
            throw new BulkApartmentValidationException(List.of(
                    new BulkApartmentValidationException.Error(0, "rows", "Provide between 1 and 500 rows")));
        }
        Map<List<String>, Integer> numbers = new HashMap<>();
        Map<String, Integer> parking = new HashMap<>();
        Map<String, Integer> storage = new HashMap<>();
        long parkingCount = apartments.countByBuilding_IdAndParkingSpaceTrueAndActiveTrue(buildingId);
        long storageCount = apartments.countByBuilding_IdAndApStorageExistTrueAndActiveTrue(buildingId);
        for (int i = 0; i < requests.size(); i++) {
            int row = i + 1;
            ApartmentRequest r = requests.get(i);
            if (r == null) {
                errors.add(new BulkApartmentValidationException.Error(row, "row", "Row must not be null"));
                continue;
            }
            validator.validate(r).forEach(v -> errors.add(new BulkApartmentValidationException.Error(row, v.getPropertyPath().toString(), v.getMessage())));
            if (r.id() != null) errors.add(new BulkApartmentValidationException.Error(row, "id", "Must be omitted or null"));
            if (r.ownerId() != null) errors.add(new BulkApartmentValidationException.Error(row, "ownerId", "Must be omitted or null"));
            if (r.residentId() != null) errors.add(new BulkApartmentValidationException.Error(row, "residentId", "Must be omitted or null"));
            if (r.isManagerHouse()) errors.add(new BulkApartmentValidationException.Error(row, "isManagerHouse", "Must be false"));
            if (r.buildingId() != null && !Objects.equals(r.buildingId(), buildingId)) errors.add(new BulkApartmentValidationException.Error(row, "buildingId", "Must match the target building"));

            check(errors, row, "row", () -> validation.validateRequiredFields(r.ownerFirstName(), r.ownerLastName(), r.number(), r.floor(), r.sqMetersApart()));
            check(errors, row, "sqMetersApart", () -> validation.validateSquareMeters(r.sqMetersApart()));
            check(errors, row, "residentFirstName", () -> validation.validateResident(r.isRented(), r.residentFirstName(), r.residentLastName()));
            for (var entry : Map.of("commonPercent", r.commonPercent() == null ? Double.NaN : r.commonPercent(),
                    "elevatorPercent", r.elevatorPercent() == null ? Double.NaN : r.elevatorPercent(),
                    "heatingPercent", r.heatingPercent() == null ? Double.NaN : r.heatingPercent()).entrySet()) {
                if (!Double.isFinite(entry.getValue()) || entry.getValue() < 0 || entry.getValue() > 1000)
                    errors.add(new BulkApartmentValidationException.Error(row, entry.getKey(), "Must be a finite number between 0 and 1000"));
            }
            check(errors, row, "number", () -> validation.validateDuplicateApartment(buildingId, r.floor(), r.number(), null));
            check(errors, row, "parkingSlot", () -> validation.validateParking(buildingId, null, false, r.parkingSpace(), r.parkingSlot(), building.getParkingSpacesNum()));
            check(errors, row, "storageSlot", () -> validation.validateStorage(buildingId, null, false, r.apStorageExist(), r.storageSlot(), building.getStorageNum()));
            String floor = key(r.floor()), number = key(r.number());
            if (floor != null && number != null) duplicate(errors, row, "number", numbers, List.of(floor, number));
            if (Boolean.TRUE.equals(r.parkingSpace())) {
                String slot = key(r.parkingSlot());
                if (slot != null) duplicate(errors, row, "parkingSlot", parking, slot);
                if (building.getParkingSpacesNum() != null && ++parkingCount > building.getParkingSpacesNum())
                    errors.add(new BulkApartmentValidationException.Error(row, "parkingSpace", "Parking capacity exceeded"));
            }
            if (r.apStorageExist()) {
                String slot = key(r.storageSlot());
                if (slot != null) duplicate(errors, row, "storageSlot", storage, slot);
                if (building.getStorageNum() != null && ++storageCount > building.getStorageNum())
                    errors.add(new BulkApartmentValidationException.Error(row, "apStorageExist", "Storage capacity exceeded"));
            }
            for (var entry : Map.of("ownerFirstName", Objects.toString(r.ownerFirstName(), ""), "ownerLastName", Objects.toString(r.ownerLastName(), ""),
                    "number", Objects.toString(r.number(), ""), "floor", Objects.toString(r.floor(), ""), "sqMetersApart", Objects.toString(r.sqMetersApart(), ""),
                    "residentFirstName", Objects.toString(r.residentFirstName(), ""), "residentLastName", Objects.toString(r.residentLastName(), ""),
                    "parkingSlot", Objects.toString(r.parkingSlot(), ""), "storageSlot", Objects.toString(r.storageSlot(), ""), "apDescription", Objects.toString(r.apDescription(), "")).entrySet()) {
                if (entry.getValue().trim().length() > 255) errors.add(new BulkApartmentValidationException.Error(row, entry.getKey(), "Must be at most 255 characters"));
            }
        }
        if (!errors.isEmpty()) throw new BulkApartmentValidationException(errors);
        List<Integer> ids = new ArrayList<>();
        for (ApartmentRequest r : requests) {
            Apartment a = mapper.toApartment(r);
            a.setBuilding(building);
            a.setOwner(null);
            a.setResident(null);
            a.setIsManagerHouse(false);
            a.setActive(true);
            a.setEnable(true);
            a.setOwnerFirstName(trim(r.ownerFirstName()));
            a.setOwnerLastName(trim(r.ownerLastName()));
            a.setNumber(trim(r.number()));
            a.setFloor(trim(r.floor()));
            a.setSqMetersApart(trim(r.sqMetersApart()));
            a.setResidentFirstName(Boolean.TRUE.equals(r.isRented()) ? trim(r.residentFirstName()) : null);
            a.setResidentLastName(Boolean.TRUE.equals(r.isRented()) ? trim(r.residentLastName()) : null);
            a.setParkingSlot(Boolean.TRUE.equals(r.parkingSpace()) ? trim(r.parkingSlot()) : null);
            a.setStorageSlot(r.apStorageExist() ? trim(r.storageSlot()) : null);
            a.setApDescription(trim(r.apDescription()));
            ids.add(apartments.save(a).getId());
        }
        apartments.flush();
        return ids;
    }

    private static String trim(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static String key(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static <T> void duplicate(List<BulkApartmentValidationException.Error> errors, int row, String field, Map<T, Integer> seen, T key) {
        Integer previous = seen.putIfAbsent(key, row);
        if (previous != null) errors.add(new BulkApartmentValidationException.Error(row, field, "Duplicate in batch (row " + previous + ")"));
    }

    private static void check(List<BulkApartmentValidationException.Error> errors, int row, String field, Runnable action) {
        try {
            action.run();
        } catch (BusinessValidationException ex) {
            errors.add(new BulkApartmentValidationException.Error(row, field, ex.getMessage()));
        }
    }
}

package com.buildingmanager.professional;

import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.professional.professionalImage.ProfessionalImageRepository;
import com.buildingmanager.professional.professionalReview.ProfessionalReviewRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalBusinessService {

    private final ProfessionalBusinessRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProfessionalImageRepository imageRepository;
    private final ProfessionalReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ProfessionalBusinessDTO> search(
            ProfessionalCategory category,
            String city
    ) {
        List<ProfessionalBusiness> businesses;

        boolean hasCategory = category != null;
        boolean hasCity = city != null && !city.isBlank();

        if (hasCategory && hasCity) {
            businesses = repository
                    .findByActiveTrueAndVerifiedTrueAndCategoryAndCityContainingIgnoreCaseOrderByCreatedAtDesc(
                            category,
                            city.trim()
                    );
        } else if (hasCategory) {
            businesses = repository
                    .findByActiveTrueAndVerifiedTrueAndCategoryOrderByCreatedAtDesc(category);
        } else if (hasCity) {
            businesses = repository
                    .findByActiveTrueAndVerifiedTrueAndCityContainingIgnoreCaseOrderByCreatedAtDesc(city.trim());
        } else {
            businesses = repository.findByActiveTrueAndVerifiedTrueOrderByCreatedAtDesc();
        }

        return businesses.stream()
                .map(this::toDTO)
                .toList();
    }

    public ProfessionalBusinessDTO register(
            ProfessionalBusinessRequest request,
            User currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ProfessionalBusiness business = ProfessionalBusiness.builder()
                .businessName(request.getBusinessName().trim())
                .ownerFullName(request.getOwnerFullName().trim())
                .category(request.getCategory())
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .country(request.getCountry())
                .city(request.getCity())
                .region(request.getRegion())
                .address(request.getAddress())
                .taxNumber(request.getTaxNumber())
                .createdByUser(user)
                .verified(false)
                .active(false)
                .ratingAverage(0.0)
                .reviewCount(0)
                .build();

        ProfessionalBusiness saved = repository.save(business);

        String payload = """
            {
              "professionalId": %d,
              "businessName": "%s",
              "tab": "professional-approval",
              "redirectUrl": "/professionals/approval"
            }
            """.formatted(
                saved.getId(),
                saved.getBusinessName().replace("\"", "\\\"")
        );

        notificationService.notifyAdmins(
                "PROFESSIONAL_PENDING_APPROVAL",
                "Νέος επαγγελματίας περιμένει έγκριση: " + saved.getBusinessName(),
                payload
        );

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalBusinessDTO> getMyBusinesses(User currentUser) {
        return repository.findByCreatedByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfessionalBusinessDTO> getPendingApproval(User currentUser) {
        requireAdmin(currentUser);

        return repository.findByVerifiedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProfessionalBusinessDTO approve(Integer id, User currentUser) {
        requireAdmin(currentUser);

        ProfessionalBusiness business = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional business not found"));

        business.setVerified(true);
        business.setActive(true);

        return toDTO(repository.save(business));
    }

    public void deactivate(Integer id, User currentUser) {
        requireAdmin(currentUser);

        ProfessionalBusiness business = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional business not found"));

        business.setActive(false);
        repository.save(business);
    }

    private void requireAdmin(User user) {
        String role = user.getRole() != null ? user.getRole().getName() : null;

        if (!"Admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Μόνο ο διαχειριστής μπορεί να εκτελέσει αυτή την ενέργεια.");
        }
    }

    private ProfessionalBusinessDTO toDTO(ProfessionalBusiness b) {

        String primaryImageUrl = imageRepository
                .findByProfessional_IdOrderByPrimaryImageDescCreatedAtDesc(b.getId())
                .stream()
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return ProfessionalBusinessDTO.builder()
                .id(b.getId())
                .businessName(b.getBusinessName())
                .ownerFullName(b.getOwnerFullName())
                .category(b.getCategory())
                .description(b.getDescription())
                .phone(b.getPhone())
                .email(b.getEmail())
                .website(b.getWebsite())
                .country(b.getCountry())
                .city(b.getCity())
                .region(b.getRegion())
                .address(b.getAddress())
                .taxNumber(b.getTaxNumber())
                .verified(b.isVerified())
                .active(b.isActive())
                .ratingAverage(b.getRatingAverage())
                .reviewCount(b.getReviewCount())
                .createdByUserId(b.getCreatedByUser() != null ? b.getCreatedByUser().getId() : null)
                .createdByUserName(b.getCreatedByUser() != null ? b.getCreatedByUser().getFullName() : null)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .primaryImageUrl(primaryImageUrl)
                .build();
    }

    public ProfessionalBusinessDTO getById(Integer id) {
        ProfessionalBusiness business = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional business not found"));

        return toDTO(business);
    }

    public ProfessionalBusinessDTO update(
            Integer id,
            ProfessionalBusinessRequest request,
            User currentUser
    ) {
        ProfessionalBusiness business = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional business not found"));

        if (
                business.getCreatedByUser() == null ||
                        !business.getCreatedByUser().getId().equals(currentUser.getId())
        ) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα επεξεργασίας αυτής της επιχείρησης.");
        }

        business.setBusinessName(request.getBusinessName().trim());
        business.setOwnerFullName(request.getOwnerFullName().trim());
        business.setCategory(request.getCategory());
        business.setDescription(request.getDescription());
        business.setPhone(request.getPhone());
        business.setEmail(request.getEmail());
        business.setWebsite(request.getWebsite());
        business.setCountry(request.getCountry());
        business.setCity(request.getCity());
        business.setRegion(request.getRegion());
        business.setAddress(request.getAddress());
        business.setTaxNumber(request.getTaxNumber());

        ProfessionalBusiness saved = repository.save(business);

        return toDTO(saved);
    }

    public ProfessionalAdminStatsDTO getAdminStats() {
        long total = repository.count();
        long pending = repository.countByVerifiedFalseAndActiveFalse();
        long approved = repository.countByVerifiedTrueAndActiveTrue();
        long inactive = repository.countByActiveFalse();
        long totalReviews = reviewRepository.count();

        return ProfessionalAdminStatsDTO.builder()
                .totalBusinesses(total)
                .pendingBusinesses(pending)
                .approvedBusinesses(approved)
                .inactiveBusinesses(inactive)
                .totalReviews(totalReviews)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ProfessionalBusinessDTO> getAdminBusinesses(
            int page,
            int size,
            User currentUser
    ) {
        requireAdmin(currentUser);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return repository.findAll(pageable)
                .map(this::toDTO);
    }

    public void deleteBusiness(Integer id, User currentUser) {
        requireAdmin(currentUser);

        ProfessionalBusiness business = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Professional business not found"));

        repository.delete(business);
    }
}

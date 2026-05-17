package com.buildingmanager.professional.professionalReview;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.professional.ProfessionalBusinessRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalReviewService {

    private final ProfessionalReviewRepository reviewRepository;
    private final ProfessionalBusinessRepository professionalRepository;
    private final UserRepository userRepository;

    public ProfessionalReviewDTO createOrUpdateReview(
            Integer professionalId,
            Integer userId,
            ProfessionalReviewRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found"));

        ProfessionalReview review = reviewRepository
                .findByProfessionalAndUser(professional, user)
                .orElseGet(() -> ProfessionalReview.builder()
                        .professional(professional)
                        .user(user)
                        .build());

        review.setRating(request.getRating());
        review.setComment(
                request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment().trim()
                        : null
        );

        ProfessionalReview saved = reviewRepository.save(review);

        recalculateProfessionalRating(professional);

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalReviewDTO> getReviews(Integer professionalId) {
        return reviewRepository.findByProfessional_IdOrderByCreatedAtDesc(professionalId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void deleteReview(Integer reviewId, Integer userId) {
        ProfessionalReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Δεν μπορείτε να διαγράψετε αξιολόγηση άλλου χρήστη.");
        }

        ProfessionalBusiness professional = review.getProfessional();

        reviewRepository.delete(review);
        reviewRepository.flush();

        recalculateProfessionalRating(professional);
    }

    private void recalculateProfessionalRating(ProfessionalBusiness professional) {
        List<ProfessionalReview> reviews =
                reviewRepository.findByProfessional_IdOrderByCreatedAtDesc(professional.getId());

        int count = reviews.size();

        double average = count == 0
                ? 0.0
                : reviews.stream()
                .mapToInt(ProfessionalReview::getRating)
                .average()
                .orElse(0.0);

        professional.setReviewCount(count);
        professional.setRatingAverage(Math.round(average * 10.0) / 10.0);

        professionalRepository.save(professional);
    }

    private ProfessionalReviewDTO toDTO(ProfessionalReview review) {
        User user = review.getUser();

        return ProfessionalReviewDTO.builder()
                .id(review.getId())
                .professionalId(review.getProfessional().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewerId(user != null ? user.getId() : null)
                .reviewerName(user != null ? user.getFullName() : null)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

package com.buildingmanager.professional.professionalReview;

import com.buildingmanager.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalReviewController {

    private final ProfessionalReviewService reviewService;

    @GetMapping("/{professionalId}/reviews")
    public List<ProfessionalReviewDTO> getReviews(
            @PathVariable Integer professionalId
    ) {
        return reviewService.getReviews(professionalId);
    }

    @PostMapping("/{professionalId}/reviews")
    public ProfessionalReviewDTO createOrUpdateReview(
            @PathVariable Integer professionalId,
            @Valid @RequestBody ProfessionalReviewRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return reviewService.createOrUpdateReview(
                professionalId,
                user.getId(),
                request
        );
    }

    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(
            @PathVariable Integer reviewId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        reviewService.deleteReview(reviewId, user.getId());
    }
}
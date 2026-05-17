package com.buildingmanager.professional.professionalFavorite;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.professional.ProfessionalBusinessDTO;
import com.buildingmanager.professional.ProfessionalBusinessRepository;
import com.buildingmanager.professional.professionalImage.ProfessionalImageRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalFavoriteService {

    private final ProfessionalFavoriteRepository favoriteRepository;
    private final ProfessionalBusinessRepository professionalRepository;
    private final UserRepository userRepository;
    private final ProfessionalImageRepository imageRepository;

    public void addFavorite(Integer professionalId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Professional not found"));

        if (favoriteRepository.existsByUserAndProfessional(user, professional)) {
            return;
        }

        ProfessionalFavorite favorite = ProfessionalFavorite.builder()
                .user(user)
                .professional(professional)
                .build();

        favoriteRepository.save(favorite);
    }

    public void removeFavorite(Integer professionalId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Professional not found"));

        favoriteRepository.findByUserAndProfessional(user, professional)
                .ifPresent(favoriteRepository::delete);
    }

    public List<ProfessionalBusinessDTO> getMyFavorites(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return favoriteRepository.findAllByUser(user)
                .stream()
                .map(favorite -> toDTO(favorite.getProfessional()))
                .toList();
    }

    private ProfessionalBusinessDTO toDTO(ProfessionalBusiness business) {
        String primaryImageUrl = imageRepository
                .findByProfessional_IdOrderByPrimaryImageDescCreatedAtDesc(business.getId())
                .stream()
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return ProfessionalBusinessDTO.builder()
                .id(business.getId())
                .businessName(business.getBusinessName())
                .ownerFullName(business.getOwnerFullName())
                .category(business.getCategory())
                .description(business.getDescription())
                .phone(business.getPhone())
                .email(business.getEmail())
                .website(business.getWebsite())
                .city(business.getCity())
                .region(business.getRegion())
                .address(business.getAddress())
                .taxNumber(business.getTaxNumber())
                .verified(business.isVerified())
                .active(business.isActive())
                .ratingAverage(business.getRatingAverage())
                .reviewCount(business.getReviewCount())
                .createdByUserId(
                        business.getCreatedByUser() != null
                                ? business.getCreatedByUser().getId()
                                : null
                )
                .primaryImageUrl(primaryImageUrl)
                .build();
    }
}
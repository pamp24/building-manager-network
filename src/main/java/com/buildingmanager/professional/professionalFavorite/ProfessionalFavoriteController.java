package com.buildingmanager.professional.professionalFavorite;

import com.buildingmanager.professional.ProfessionalBusinessDTO;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professionals/favorites")
@RequiredArgsConstructor
public class ProfessionalFavoriteController {

    private final ProfessionalFavoriteService favoriteService;

    @PostMapping("/{professionalId}")
    public void addFavorite(
            @PathVariable Integer professionalId,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        favoriteService.addFavorite(
                professionalId,
                user.getId()
        );
    }

    @DeleteMapping("/{professionalId}")
    public void removeFavorite(
            @PathVariable Integer professionalId,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        favoriteService.removeFavorite(
                professionalId,
                user.getId()
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProfessionalBusinessDTO>> getMyFavorites(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                favoriteService.getMyFavorites(user.getId())
        );
    }
}

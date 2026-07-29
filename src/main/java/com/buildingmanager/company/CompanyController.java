package com.buildingmanager.company;

import com.buildingmanager.building.Building;
import com.buildingmanager.exceptions.UserNotFoundException;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CompanyController(CompanyService companyService, UserRepository userRepository, CompanyRepository companyRepository) {
        this.companyService = companyService;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Company> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id) {
        return companyService.getCompanyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody Company company) {
        CompanyDTO dto = companyService.createCompanyAndPromoteCurrentUser(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/my/company")
    public ResponseEntity<CompanyDTO> updateMyCompany(@RequestBody CompanyDTO dto) {
        CompanyDTO updated = companyService.updateMyCompany(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Company> createCompanyForUser(@PathVariable Integer userId, @RequestBody Company company) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id '" + userId + "' not found"));

        if (!user.getRole().equals("PropertyManager")) {
            throw new org.springframework.security.access.AccessDeniedException("User is not a PropertyManager");
        }

        Company saved = companyService.createCompany(company);
        user.setCompany(saved);
        userRepository.save(user);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{companyId}/buildings")
    public ResponseEntity<List<Building>> getBuildingsByCompany(@PathVariable Long companyId) {
        return companyRepository.findById(companyId)
                .map(company -> ResponseEntity.ok(company.getBuildings()))
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/my/company")
    public ResponseEntity<CompanyDTO> getMyCompany() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("User is not authenticated");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Company company = user.getCompany();
        if (company == null) {
            throw new EntityNotFoundException("No company found for current user");
        }

        return ResponseEntity.ok(companyService.toDto(company));
    }

}

package com.buildingmanager.company;

import com.buildingmanager.building.Building;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Company> createCompany(@RequestBody Company company) {
        return ResponseEntity.ok(companyService.createCompany(company));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable Long id, @RequestBody Company updatedCompany) {
        return ResponseEntity.ok(companyService.updateCompany(id, updatedCompany));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Company> createCompanyForUser(@PathVariable Integer userId, @RequestBody Company company) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();

        if (!user.getRole().equals("PropertyManager")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
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

}


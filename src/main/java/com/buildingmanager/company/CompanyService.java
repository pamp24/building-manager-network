package com.buildingmanager.company;


import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository, RoleRepository roleRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public CompanyDTO toDto(Company c) {
        CompanyDTO dto = new CompanyDTO();
        dto.setCompanyId(c.getId());
        dto.setCompanyName(c.getCompanyName());
        dto.setTaxNumber(c.getTaxNumber());
        dto.setManagerName(c.getManagerName());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setAddress(c.getAddress());
        dto.setAddressNumber(c.getAddressNumber());
        dto.setPostalCode(c.getPostalCode());
        dto.setCity(c.getCity());
        dto.setRegion(c.getRegion());
        dto.setCountry(c.getCountry());
        return dto;
    }


    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    public CompanyDTO createCompanyAndPromoteCurrentUser(Company company) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Company saved = companyRepository.save(company);

        Role pmRole = roleRepository.findById(5)
                .orElseThrow(() -> new RuntimeException("Role id=5 (PropertyManager) not found"));

        user.setCompany(saved);
        user.setRole(pmRole);
        userRepository.save(user);

        return toDto(saved);
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    public CompanyDTO updateMyCompany(CompanyDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Company company = user.getCompany();
        if (company == null) {
            throw new jakarta.persistence.EntityNotFoundException("No company linked to user");
        }

        // update fields (μόνο όσα επιτρέπεις να αλλάζουν)
        company.setCompanyName(dto.getCompanyName());
        company.setTaxNumber(dto.getTaxNumber());
        company.setManagerName(dto.getManagerName());
        company.setEmail(dto.getEmail());
        company.setPhone(dto.getPhone());
        company.setAddress(dto.getAddress());
        company.setAddressNumber(dto.getAddressNumber());
        company.setPostalCode(dto.getPostalCode());
        company.setCity(dto.getCity());
        company.setRegion(dto.getRegion());
        company.setCountry(dto.getCountry()); // αν το έχεις στο backend DTO

        Company saved = companyRepository.save(company);
        return toDto(saved);
    }
}


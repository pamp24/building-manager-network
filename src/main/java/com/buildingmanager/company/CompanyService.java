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
        dto.setCompanyId(c.getId());          // από BaseEntity
        dto.setCompanyName(c.getCompanyName());
        dto.setTaxNumber(c.getTaxNumber());
        dto.setManagerName(c.getManagerName());
        dto.setEmail(c.getEmail());
        dto.setPhone(c.getPhone());
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

    public Company updateCompany(Long id, Company updated) {
        return companyRepository.findById(id).map(company -> {
            company.setCompanyName(updated.getCompanyName());
            company.setTaxNumber(updated.getTaxNumber());
            company.setManagerName(updated.getManagerName());
            company.setEmail(updated.getEmail());
            company.setPhone(updated.getPhone());
            company.setAddress(updated.getAddress());
            company.setAddressNumber(updated.getAddressNumber());
            company.setPostalCode(updated.getPostalCode());
            company.setCity(updated.getCity());
            company.setRegion(updated.getRegion());
            company.setCountry(updated.getCountry());
            return companyRepository.save(company);
        }).orElseThrow(() -> new RuntimeException("Company not found"));
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }
}


package com.buildingmanager.company;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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


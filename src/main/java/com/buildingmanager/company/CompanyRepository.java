package com.buildingmanager.company;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByTaxNumber(String taxNumber);
}


package com.buildingmanager.building;

import org.springframework.data.jpa.domain.Specification;

public class BuildingSpecification {
    public static Specification<Building> withManagerId(Integer managerId){
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("manager").get("id"), managerId);
    }
}

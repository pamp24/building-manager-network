package com.buildingmanager.professional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProfessionalBusinessSpecification {

    private ProfessionalBusinessSpecification() {
    }

    public static Specification<ProfessionalBusiness> filter(
            ProfessionalCategory category,
            String country,
            String region,
            String city,
            String area
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.isTrue(root.get("verified")));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (country != null && !country.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("country")),
                                country.toLowerCase()
                        )
                );
            }

            if (region != null && !region.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("region")),
                                "%" + region.toLowerCase() + "%"
                        )
                );
            }

            if (city != null && !city.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("city")),
                                "%" + city.toLowerCase() + "%"
                        )
                );
            }

            if (area != null && !area.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("area")),
                                "%" + area.toLowerCase() + "%"
                        )
                );
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
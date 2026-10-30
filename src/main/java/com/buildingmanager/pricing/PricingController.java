package com.buildingmanager.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/pricing")
@EnableConfigurationProperties(PricingProperties.class)
public class PricingController {
    private final PricingProperties properties;

    public PricingController(PricingProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Catalog catalog() {
        return new Catalog("EUR", properties.annualDiscountBasisPoints(), properties.businessVatBasisPoints(), plans());
    }

    private List<Plan> plans() {
        return List.of(
                new Plan("INDIVIDUAL_FREE", "INCLUSIVE", false, 1, 5, 5, 0L, 0L),
                new Plan("INDIVIDUAL_PLUS", "INCLUSIVE", false, 1, null, 5, properties.individualPlusMonthlyCents(), properties.individualPlusExtraApartmentCents()),
                new Plan("INDIVIDUAL_EXTENDED", "INCLUSIVE", true, null, null, null, null, null),
                new Plan("BUSINESS_STARTER", "EXCLUSIVE", false, 5, 50, 50, properties.businessStarterMonthlyCents(), 0L),
                new Plan("BUSINESS_GROWTH", "EXCLUSIVE", false, 20, 250, 250, properties.businessGrowthMonthlyCents(), 0L),
                new Plan("BUSINESS_ENTERPRISE", "EXCLUSIVE", true, null, null, null, null, null));
    }

    @PostMapping("/quote")
    public Quote quote(@Valid @RequestBody QuoteRequest request) {
        Plan plan = plans().stream().filter(p -> p.id().equals(request.planId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown plan ID"));
        if ((plan.maxBuildings() != null && request.buildings() > plan.maxBuildings()) ||
                (plan.maxApartments() != null && request.apartments() > plan.maxApartments())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected counts exceed plan limits");
        }
        if (plan.quoteOnly()) {
            return new Quote(plan.id(), request.buildings(), request.apartments(), true, "EUR", null, null);
        }
        long extras = Math.max(0, request.apartments() - plan.includedApartments());
        long monthly = plan.monthlyBaseCents() + extras * plan.extraApartmentMonthlyCents();
        long annual = round(monthly * 12, 10000 - properties.annualDiscountBasisPoints(), 10000);
        return new Quote(plan.id(), request.buildings(), request.apartments(), false, "EUR",
                price(monthly, plan.taxTreatment()), price(annual, plan.taxTreatment()));
    }

    private Price price(long amount, String taxTreatment) {
        if (taxTreatment.equals("INCLUSIVE")) {
            // The individual VAT rate is not specified; never invent a net/tax split.
            return new Price(null, null, amount);
        }
        long vat = round(amount, properties.businessVatBasisPoints(), 10000);
        return new Price(amount, vat, amount + vat);
    }

    private static long round(long amount, long numerator, long denominator) {
        return (amount * numerator + denominator / 2) / denominator;
    }

    public record Catalog(String currency, int annualDiscountBasisPoints, int businessVatBasisPoints, List<Plan> plans) {}
    public record Plan(String id, String taxTreatment, boolean quoteOnly, Integer maxBuildings,
                       Integer maxApartments, Integer includedApartments, Long monthlyBaseCents,
                       Long extraApartmentMonthlyCents) {}
    public record QuoteRequest(@NotNull String planId, @NotNull @Min(1) @Max(100000) Integer buildings,
                               @NotNull @Min(1) @Max(100000) Integer apartments) {}
    public record Quote(String planId, int buildings, int apartments, boolean quoteOnly, String currency,
                        Price monthly, Price annual) {}
    public record Price(Long netCents, Long vatCents, Long grossCents) {}
}

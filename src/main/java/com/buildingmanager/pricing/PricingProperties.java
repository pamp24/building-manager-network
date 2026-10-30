package com.buildingmanager.pricing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(
        @DefaultValue("1500") @Min(0) @Max(10000) int annualDiscountBasisPoints,
        @DefaultValue("2400") @Min(0) @Max(10000) int businessVatBasisPoints,
        @DefaultValue("990") @Min(0) @Max(100000000) long individualPlusMonthlyCents,
        @DefaultValue("100") @Min(0) @Max(100000000) long individualPlusExtraApartmentCents,
        @DefaultValue("2990") @Min(0) @Max(100000000) long businessStarterMonthlyCents,
        @DefaultValue("7990") @Min(0) @Max(100000000) long businessGrowthMonthlyCents) {}

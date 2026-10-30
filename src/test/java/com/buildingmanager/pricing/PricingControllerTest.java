package com.buildingmanager.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PricingControllerTest {
    private final PricingController controller = new PricingController(new PricingProperties(1500, 2400, 990, 100, 2990, 7990));

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PricingProperties.class)
    static class TestConfiguration {}

    @Test
    void validatesConfigurationAndDefaults() {
        var context = new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class);
        context.run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(PricingProperties.class).businessVatBasisPoints()).isEqualTo(2400);
            assertThat(result.getBean(PricingProperties.class).annualDiscountBasisPoints()).isEqualTo(1500);
        });
        context.withPropertyValues("pricing.business-vat-basis-points=-1").run(result -> assertThat(result).hasFailed());
        context.withPropertyValues("pricing.annual-discount-basis-points=10001").run(result -> assertThat(result).hasFailed());
        context.withPropertyValues("pricing.individual-plus-monthly-cents=-1").run(result -> assertThat(result).hasFailed());
    }

    @Test
    void catalogHasAllSpecifiedPlans() {
        assertThat(controller.catalog().plans()).extracting(PricingController.Plan::id).containsExactly(
                "INDIVIDUAL_FREE", "INDIVIDUAL_PLUS", "INDIVIDUAL_EXTENDED",
                "BUSINESS_STARTER", "BUSINESS_GROWTH", "BUSINESS_ENTERPRISE");
        assertThat(controller.catalog().plans().get(0).maxApartments()).isEqualTo(5);
        assertThat(controller.catalog().plans().get(4).maxBuildings()).isEqualTo(20);
    }

    @Test
    void calculatesInclusivePlusAndAnnualDiscount() {
        var quote = controller.quote(new PricingController.QuoteRequest("INDIVIDUAL_PLUS", 1, 7));
        assertThat(quote.monthly()).isEqualTo(new PricingController.Price(null, null, 1190L));
        assertThat(quote.annual()).isEqualTo(new PricingController.Price(null, null, 12138L));
        assertThat(controller.quote(new PricingController.QuoteRequest("INDIVIDUAL_FREE", 1, 5)).monthly().grossCents()).isZero();
    }

    @Test
    void calculatesBusinessVatAndDiscount() {
        var quote = controller.quote(new PricingController.QuoteRequest("BUSINESS_STARTER", 5, 50));
        assertThat(quote.monthly()).isEqualTo(new PricingController.Price(2990L, 718L, 3708L));
        assertThat(quote.annual()).isEqualTo(new PricingController.Price(30498L, 7320L, 37818L));
        assertThat(controller.quote(new PricingController.QuoteRequest("BUSINESS_GROWTH", 20, 250)).monthly().netCents()).isEqualTo(7990L);
    }

    @Test
    void quoteOnlyPlansNeverReturnNumericPrices() {
        for (String id : new String[]{"INDIVIDUAL_EXTENDED", "BUSINESS_ENTERPRISE"}) {
            var quote = controller.quote(new PricingController.QuoteRequest(id, 50, 300));
            assertThat(quote.quoteOnly()).isTrue();
            assertThat(quote.monthly()).isNull();
            assertThat(quote.annual()).isNull();
        }
    }

    @Test
    void rejectsInvalidRequestsAndLimits() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String body : new String[]{
                "{\"planId\":\"UNKNOWN\",\"buildings\":1,\"apartments\":1}",
                "{\"planId\":\"INDIVIDUAL_FREE\",\"buildings\":1,\"apartments\":6}",
                "{\"planId\":\"INDIVIDUAL_PLUS\",\"buildings\":2,\"apartments\":1}",
                "{\"planId\":\"BUSINESS_GROWTH\",\"buildings\":20,\"apartments\":251}",
                "{\"planId\":\"BUSINESS_STARTER\",\"buildings\":0,\"apartments\":1}",
                "{\"planId\":\"BUSINESS_STARTER\",\"buildings\":1}"}) {
            mvc.perform(post("/api/v1/pricing/quote").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}

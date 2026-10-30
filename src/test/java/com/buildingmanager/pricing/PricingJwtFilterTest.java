package com.buildingmanager.pricing;

import com.buildingmanager.security.JwtFilter;
import com.buildingmanager.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.*;

class PricingJwtFilterTest {
    @Test
    void publicPricingRequestsIgnoreStaleBearerTokens() throws Exception {
        var jwt = mock(JwtService.class);
        var filter = new JwtFilter(jwt, mock(UserDetailsService.class), new ObjectMapper());
        for (String[] route : new String[][]{{"GET", "/api/v1/pricing"}, {"POST", "/api/v1/pricing/quote"}}) {
            var request = new MockHttpServletRequest(route[0], route[1]);
            request.setServletPath(route[1]);
            request.addHeader("Authorization", "Bearer stale");
            var response = new MockHttpServletResponse();
            var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
        }
        verifyNoInteractions(jwt);
    }
}

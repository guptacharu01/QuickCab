package com.cabbooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security smoke tests against an in-memory H2 database:
 * - protected endpoints reject anonymous callers,
 * - admin endpoints reject non-admin roles (tested via missing auth -> 401/403),
 * - the Stripe webhook stays public (Stripe must reach it).
 * Run with: mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-256"
})
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousCannotListRides() throws Exception {
        mvc.perform(get("/api/rides/mine")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReachAdminStats() throws Exception {
        mvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotSeeDriverRequests() throws Exception {
        mvc.perform(get("/api/driver/requests")).andExpect(status().isForbidden());
    }

    @Test
    void webhookEndpointIsPublic() throws Exception {
        // No auth header: Stripe must be able to call it. A bad signature gives
        // 400/503 (not 401/403), proving the route is not behind login.
        mvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "bad")
                        .content("{}")
                        .contentType("application/json"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            s != 401 && s != 403, "webhook must be public, got " + s);
                });
    }

    @Test
    void authEndpointsArePublic() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}")
                        .contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }
}

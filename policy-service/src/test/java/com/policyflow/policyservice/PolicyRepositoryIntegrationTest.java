package com.policyflow.policyservice;

import com.policyflow.policyservice.entity.Policy;
import com.policyflow.policyservice.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {"app.security.username=test-user", "app.security.password=test-password"})
@Testcontainers
class PolicyRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("policyflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    void savesAndFindsPolicyByPolicyNumber() {
        Policy policy = new Policy();
        policy.setPolicyNumber("POL-IT-10001");
        policy.setCustomerId("CUS-10001");
        policy.setPolicyType("AUTO");
        policy.setPremium(new BigDecimal("125.00"));
        policy.setStatus("ACTIVE");

        policyRepository.saveAndFlush(policy);

        Optional<Policy> result =
                policyRepository.findByPolicyNumber("POL-IT-10001");

        assertTrue(result.isPresent());
        assertEquals("POL-IT-10001", result.get().getPolicyNumber());
        assertEquals(new BigDecimal("125.00"), result.get().getPremium());
    }
}

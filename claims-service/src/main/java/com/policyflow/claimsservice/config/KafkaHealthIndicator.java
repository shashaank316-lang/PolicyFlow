
package com.policyflow.claimsservice.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private final AdminClient adminClient;

    public KafkaHealthIndicator(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public Health health() {
        try {
            adminClient.describeCluster()
                    .nodes()
                    .get(3, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("broker", "Kafka is reachable")
                    .build();

        } catch (Exception ex) {
            return Health.down()
                    .withDetail("broker", "Kafka is unavailable")
                    .withDetail("error", ex.getClass().getSimpleName())
                    .build();
        }
    }
}

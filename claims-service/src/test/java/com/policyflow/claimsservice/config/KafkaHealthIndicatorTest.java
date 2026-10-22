
package com.policyflow.claimsservice.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaHealthIndicatorTest {

    @Test
    void reportsUpWhenKafkaIsReachable() throws Exception {
        AdminClient adminClient = mock(AdminClient.class);
        DescribeClusterResult clusterResult =
                mock(DescribeClusterResult.class);

        when(adminClient.describeCluster()).thenReturn(clusterResult);

        when(clusterResult.nodes()).thenReturn(
                KafkaFuture.completedFuture(
                        List.of(new Node(1, "localhost", 9092))
                )
        );

        KafkaHealthIndicator indicator =
                new KafkaHealthIndicator(adminClient);

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void reportsDownWhenKafkaIsUnavailable() {
        AdminClient adminClient = mock(AdminClient.class);

        when(adminClient.describeCluster()).thenThrow(
                new RuntimeException("Kafka unavailable")
        );

        KafkaHealthIndicator indicator =
                new KafkaHealthIndicator(adminClient);

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}

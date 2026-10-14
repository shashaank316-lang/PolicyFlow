package com.policyflow.claimsservice.config;

import com.policyflow.claimsservice.event.ClaimCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private Map<String, Object> producerProperties(
            Class<?> valueSerializer) {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                valueSerializer
        );

        return properties;
    }

    @Bean
    public ProducerFactory<String, ClaimCreatedEvent>
    claimEventProducerFactory() {

        return new DefaultKafkaProducerFactory<>(
                producerProperties(JacksonJsonSerializer.class)
        );
    }

    @Bean
    public KafkaTemplate<String, ClaimCreatedEvent>
    claimEventKafkaTemplate() {

        return new KafkaTemplate<>(claimEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {

        return new DefaultKafkaProducerFactory<>(
                producerProperties(StringSerializer.class)
        );
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {

        return new KafkaTemplate<>(outboxProducerFactory());
    }
}

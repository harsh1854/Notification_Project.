package com.harsh.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 2L)
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                System.out.println(
                        "Retry attempt: " + deliveryAttempt
                                + " | Topic: " + record.topic()
                                + " | Error: " + ex.getMessage()
                )
        );

        return errorHandler;
    }
}

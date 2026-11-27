package com.ismael.CoopPay;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Um par de containers para toda a suíte: um par por contexto Spring esgota a CI (timeout do Kafka).
	// Start é idempotente; o Ryuk remove os containers ao fim da JVM.
	private static final KafkaContainer KAFKA =
			new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

	static {
		KAFKA.start();
		POSTGRES.start();
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return KAFKA;
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return POSTGRES;
	}

}

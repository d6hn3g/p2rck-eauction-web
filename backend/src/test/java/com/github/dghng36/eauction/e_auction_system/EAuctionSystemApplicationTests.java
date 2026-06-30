package com.github.dghng36.eauction.e_auction_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dghng36.eauction.boot.EAuctionSystemApplication;
import com.github.dghng36.eauction.e_auction_system.integration.support.IntegrationTestConfig;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = EAuctionSystemApplication.class)
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
class EAuctionSystemApplicationTests {

	@Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
	}

	@Test
	void contextLoads() {
	}
}

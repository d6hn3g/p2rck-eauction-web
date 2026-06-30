package com.github.dghng36.eauction.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.github.dghng36.eauction")
@EnableMongoRepositories(basePackages = "com.github.dghng36.eauction")
@EnableAsync
public class EAuctionSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(EAuctionSystemApplication.class, args);
	}

}

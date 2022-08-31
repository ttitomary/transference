package com.bank.transference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class TransferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransferenceApplication.class, args);
	}

}

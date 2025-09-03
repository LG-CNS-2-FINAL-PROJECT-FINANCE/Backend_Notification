package com.ddiring.Backend_Notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class BackendNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendNotificationApplication.class, args);
	}

}

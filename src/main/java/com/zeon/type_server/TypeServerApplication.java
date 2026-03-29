package com.zeon.type_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EntityScan("com.zeon.type_server.dao.model")
public class TypeServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TypeServerApplication.class, args);
	}

}

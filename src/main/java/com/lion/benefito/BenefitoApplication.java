package com.lion.benefito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BenefitoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BenefitoApplication.class, args);
	}

}

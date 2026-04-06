package com.api.konditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KonditorApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonditorApplication.class, args);
	}

}

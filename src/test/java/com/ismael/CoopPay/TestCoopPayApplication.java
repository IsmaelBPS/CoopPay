package com.ismael.CoopPay;

import org.springframework.boot.SpringApplication;

public class TestCoopPayApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoopPayApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

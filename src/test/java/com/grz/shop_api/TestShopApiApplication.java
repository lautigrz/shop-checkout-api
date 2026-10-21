package com.grz.shop_api;

import org.springframework.boot.SpringApplication;

public class TestShopApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShopApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

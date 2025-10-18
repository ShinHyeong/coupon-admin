package com.coupon.system.coupon_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CouponAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(CouponAdminApplication.class, args);
	}

}

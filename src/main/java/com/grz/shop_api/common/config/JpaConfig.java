package com.grz.shop_api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
        "com.grz.shop_api.user.infrastructure",
        "com.grz.shop_api.order.infrastructure"
})
public class JpaConfig {
}

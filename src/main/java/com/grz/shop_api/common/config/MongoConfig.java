package com.grz.shop_api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = {
        "com.grz.shop_api.product.infrastructure",
        "com.grz.shop_api.order.infrastructure"
})
public class MongoConfig {
}

package com.plantmarket.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

  @Bean
  public RouteLocator routes(
      RouteLocatorBuilder builder,
      @Value("${app.gateway.auth-uri}") String authUri,
      @Value("${app.gateway.catalog-uri}") String catalogUri) {
    return builder
        .routes()
        .route("auth-service", route -> route.path("/api/v1/auth/**").uri(authUri))
        .route(
            "catalog-service",
            route -> route.path("/api/v1/categories/**", "/api/v1/products/**").uri(catalogUri))
        .build();
  }
}

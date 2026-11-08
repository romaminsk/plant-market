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
      @Value("${app.gateway.catalog-uri}") String catalogUri,
      @Value("${app.gateway.order-uri}") String orderUri) {
    return builder
        .routes()
        .route("auth-service", route -> route.path("/api/v1/auth/**").uri(authUri))
        .route("media", route -> route.path("/media/**").uri(catalogUri))
        .route(
            "catalog-service",
            route ->
                route
                    .path(
                        "/api/v1/categories/**",
                        "/api/v1/products/**",
                        "/api/v1/variants/**",
                        "/api/v1/individual-plants/**",
                        "/api/v1/stock-batches/**",
                        "/api/v1/restock-subscriptions/**",
                        "/api/v1/media/**")
                    .uri(catalogUri))
        .route(
            "order-service",
            route ->
                route
                    .path(
                        "/api/v1/orders/**",
                        "/api/v1/shipping/**",
                        "/api/v1/claims/**",
                        "/api/v1/care-reminders/**",
                        "/api/v1/admin/**")
                    .uri(orderUri))
        .build();
  }
}

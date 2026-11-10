package com.plantmarket.catalog.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(
    name = "app.media.storage",
    havingValue = "filesystem",
    matchIfMissing = true)
public class MediaWebConfig implements WebMvcConfigurer {

  private final String root;

  public MediaWebConfig(@Value("${app.media.root:./data/media}") String root) {
    this.root = root;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(root).toAbsolutePath().normalize().toUri().toString();
    if (!location.endsWith("/")) {
      location = location + "/";
    }
    registry.addResourceHandler("/media/**").addResourceLocations(location);
  }
}

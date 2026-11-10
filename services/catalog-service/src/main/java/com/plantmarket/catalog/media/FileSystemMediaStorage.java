package com.plantmarket.catalog.media;

import com.plantmarket.common.id.UuidV7;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Phase 3 media storage on a mounted volume. MinIO/S3 can replace this bean without changing the
 * upload API (ADR-0011).
 */
@Component
@ConditionalOnProperty(
    name = "app.media.storage",
    havingValue = "filesystem",
    matchIfMissing = true)
public class FileSystemMediaStorage implements MediaStorage {

  private final Path root;
  private final String publicBaseUrl;

  public FileSystemMediaStorage(
      @Value("${app.media.root:./data/media}") String root,
      @Value("${app.media.public-base-url:http://localhost:8081/media}") String publicBaseUrl) {
    this.root = Path.of(root).toAbsolutePath().normalize();
    this.publicBaseUrl =
        publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
  }

  @Override
  public String store(byte[] content, String contentType, String extension) {
    String name = UuidV7.generate() + "." + extension;
    try {
      Files.createDirectories(root);
      Files.write(root.resolve(name), content);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not store media file", exception);
    }
    return publicBaseUrl + "/" + name;
  }

  @Override
  public Optional<StoredMedia> load(String key) {
    Path file = root.resolve(key).normalize();
    if (!file.startsWith(root) || !Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      String contentType = Files.probeContentType(file);
      return Optional.of(
          new StoredMedia(
              Files.readAllBytes(file),
              contentType == null ? "application/octet-stream" : contentType));
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read media file", exception);
    }
  }
}

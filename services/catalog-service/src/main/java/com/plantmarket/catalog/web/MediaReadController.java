package com.plantmarket.catalog.web;

import com.plantmarket.catalog.media.MediaStorage;
import com.plantmarket.common.error.ApiException;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** S3 media is private; catalog-service streams it through the gateway's /media route. */
@RestController
@ConditionalOnProperty(name = "app.media.storage", havingValue = "s3")
public class MediaReadController {

  private final MediaStorage mediaStorage;

  public MediaReadController(MediaStorage mediaStorage) {
    this.mediaStorage = mediaStorage;
  }

  @GetMapping("/media/{key}")
  public ResponseEntity<byte[]> read(@PathVariable String key) {
    if (!key.matches("[0-9a-f-]{36}\\.(png|jpg|webp)")) {
      throw ApiException.notFound("Media not found: " + key);
    }
    MediaStorage.StoredMedia media =
        mediaStorage.load(key).orElseThrow(() -> ApiException.notFound("Media not found: " + key));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(media.contentType()))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .body(media.content());
  }
}

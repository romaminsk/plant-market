package com.plantmarket.catalog.web;

import com.plantmarket.catalog.media.MediaStorage;
import com.plantmarket.catalog.web.dto.MediaResponse;
import com.plantmarket.common.error.ApiException;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

  private static final long MAX_BYTES = 10L * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES =
      Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp");

  private final MediaStorage mediaStorage;

  public MediaController(MediaStorage mediaStorage) {
    this.mediaStorage = mediaStorage;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public MediaResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      throw ApiException.badRequest("File is empty");
    }
    if (file.getSize() > MAX_BYTES) {
      throw ApiException.badRequest("File exceeds 10 MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
      throw ApiException.badRequest("Only JPEG, PNG and WebP images are allowed");
    }
    String extension =
        switch (contentType) {
          case MediaType.IMAGE_JPEG_VALUE -> "jpg";
          case MediaType.IMAGE_PNG_VALUE -> "png";
          default -> "webp";
        };
    byte[] content = file.getBytes();
    if (!matchesSignature(content, contentType)) {
      throw ApiException.badRequest("File content does not match its content type");
    }
    return new MediaResponse(mediaStorage.store(content, contentType, extension));
  }

  private static boolean matchesSignature(byte[] content, String contentType) {
    if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
      return content.length >= 8
          && (content[0] & 0xFF) == 0x89
          && content[1] == 'P'
          && content[2] == 'N'
          && content[3] == 'G'
          && content[4] == 0x0D
          && content[5] == 0x0A
          && content[6] == 0x1A
          && content[7] == 0x0A;
    }
    if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
      return content.length >= 3
          && (content[0] & 0xFF) == 0xFF
          && (content[1] & 0xFF) == 0xD8
          && (content[2] & 0xFF) == 0xFF;
    }
    return content.length >= 12
        && content[0] == 'R'
        && content[1] == 'I'
        && content[2] == 'F'
        && content[3] == 'F'
        && content[8] == 'W'
        && content[9] == 'E'
        && content[10] == 'B'
        && content[11] == 'P';
  }
}

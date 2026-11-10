package com.plantmarket.catalog.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3MediaStorageTest {

  @Test
  void storesObjectAndReturnsPublicUrl() {
    S3Client client = mock(S3Client.class);
    S3MediaStorage storage =
        new S3MediaStorage(client, "plant-market-media", "https://staging.test/media/");

    String url = storage.store("png".getBytes(StandardCharsets.UTF_8), "image/png", "png");

    assertThat(url).startsWith("https://staging.test/media/").endsWith(".png");
    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(request.capture(), any(RequestBody.class));
    assertThat(request.getValue().bucket()).isEqualTo("plant-market-media");
    assertThat(request.getValue().key()).endsWith(".png");
    assertThat(request.getValue().contentType()).isEqualTo("image/png");
  }

  @Test
  void loadsObjectBytesAndContentType() {
    S3Client client = mock(S3Client.class);
    byte[] bytes = "png".getBytes(StandardCharsets.UTF_8);
    when(client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenReturn(
            ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("image/png").build(), bytes));
    S3MediaStorage storage =
        new S3MediaStorage(client, "plant-market-media", "https://staging.test/media");

    S3MediaStorage.StoredMedia media = storage.load("abc.png").orElseThrow();

    assertThat(media.content()).isEqualTo(bytes);
    assertThat(media.contentType()).isEqualTo("image/png");
  }
}

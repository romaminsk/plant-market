package com.plantmarket.catalog.media;

import com.plantmarket.common.id.UuidV7;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** S3/MinIO storage; keys stay under media/ so the bucket can be served read-only. */
@Component
@ConditionalOnProperty(name = "app.media.storage", havingValue = "s3")
public class S3MediaStorage implements MediaStorage {

  private final S3Client s3;
  private final String bucket;
  private final String publicBaseUrl;

  @Autowired
  public S3MediaStorage(
      @Value("${app.media.s3.endpoint}") String endpoint,
      @Value("${app.media.s3.region:us-east-1}") String region,
      @Value("${app.media.s3.bucket}") String bucket,
      @Value("${app.media.s3.access-key}") String accessKey,
      @Value("${app.media.s3.secret-key}") String secretKey,
      @Value("${app.media.public-base-url}") String publicBaseUrl) {
    this(
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true)
            .build(),
        bucket,
        publicBaseUrl);
  }

  S3MediaStorage(S3Client s3, String bucket, String publicBaseUrl) {
    this.s3 = s3;
    this.bucket = bucket;
    this.publicBaseUrl =
        publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
  }

  @Override
  public String store(byte[] content, String contentType, String extension) {
    String key = UuidV7.generate() + "." + extension;
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
        RequestBody.fromBytes(content));
    return publicBaseUrl + "/" + key;
  }

  @Override
  public Optional<StoredMedia> load(String key) {
    try {
      var response =
          s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
      return Optional.of(
          new StoredMedia(
              response.asByteArray(),
              response.response().contentType() == null
                  ? "application/octet-stream"
                  : response.response().contentType()));
    } catch (NoSuchKeyException exception) {
      return Optional.empty();
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return Optional.empty();
      }
      throw exception;
    }
  }
}

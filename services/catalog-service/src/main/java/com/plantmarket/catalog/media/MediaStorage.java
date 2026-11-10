package com.plantmarket.catalog.media;

import java.util.Optional;

public interface MediaStorage {

  String store(byte[] content, String contentType, String extension);

  Optional<StoredMedia> load(String key);

  record StoredMedia(byte[] content, String contentType) {}
}

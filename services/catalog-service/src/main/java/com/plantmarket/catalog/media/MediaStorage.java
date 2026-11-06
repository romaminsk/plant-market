package com.plantmarket.catalog.media;

public interface MediaStorage {

  String store(byte[] content, String contentType, String extension);
}

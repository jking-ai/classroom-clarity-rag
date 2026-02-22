package com.jkingai.classroomclarity.service;

public interface StorageService {

    String store(String filename, byte[] content);

    void delete(String storagePath);
}

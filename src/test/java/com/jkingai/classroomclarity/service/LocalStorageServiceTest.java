package com.jkingai.classroomclarity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalStorageService(tempDir.resolve("uploads").toString());
    }

    @Test
    void storeCreatesFileAndReturnsPath() throws IOException {
        byte[] content = "PDF content here".getBytes();

        String storagePath = service.store("test.pdf", content);

        assertThat(storagePath).isNotBlank();
        assertThat(Files.exists(Path.of(storagePath))).isTrue();
        assertThat(Files.readAllBytes(Path.of(storagePath))).isEqualTo(content);
    }

    @Test
    void storeGeneratesUniqueFilenames() {
        byte[] content = "content".getBytes();

        String path1 = service.store("test.pdf", content);
        String path2 = service.store("test.pdf", content);

        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void deleteRemovesFile() {
        byte[] content = "to be deleted".getBytes();
        String storagePath = service.store("delete-me.pdf", content);

        service.delete(storagePath);

        assertThat(Files.exists(Path.of(storagePath))).isFalse();
    }

    @Test
    void deleteNonExistentFileDoesNotThrow() {
        service.delete("/nonexistent/path/file.pdf");
        // Should not throw
    }

    @Test
    void autoCreatesDirectoryIfMissing() {
        Path nestedDir = tempDir.resolve("nested/deep/uploads");
        LocalStorageService nested = new LocalStorageService(nestedDir.toString());

        String path = nested.store("test.pdf", "content".getBytes());

        assertThat(Files.exists(Path.of(path))).isTrue();
    }
}

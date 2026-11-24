package ug.caa.adpautomate.attachment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ug.caa.adpautomate.web.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFileStorage {

  private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

  private final Path root;

  public LocalFileStorage(@Value("${app.upload-dir}") String uploadDir) {
    Path configured = Path.of(uploadDir).toAbsolutePath().normalize();
    this.root = ensureWritable(configured);
  }

  private static Path ensureWritable(Path preferred) {
    try {
      Files.createDirectories(preferred);
      if (Files.isWritable(preferred)) {
        return preferred;
      }
    } catch (IOException ex) {
      log.warn("Upload dir {} is not usable ({}), falling back to temp.", preferred, ex.toString());
    }
    Path fallback = Path.of(System.getProperty("java.io.tmpdir"), "adp-automate-uploads")
        .toAbsolutePath()
        .normalize();
    try {
      Files.createDirectories(fallback);
      log.warn("Using fallback upload directory: {}", fallback);
      return fallback;
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot create upload directory: " + preferred + " or " + fallback, ex);
    }
  }

  public void store(String storageKey, InputStream input) {
    Path target = resolve(storageKey);
    try {
      Files.createDirectories(target.getParent());
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "storage", "Could not store the uploaded file.");
    }
  }

  public Resource load(String storageKey) {
    Path target = resolve(storageKey);
    if (!Files.isRegularFile(target)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "not-found", "Stored file is missing.");
    }
    return new FileSystemResource(target);
  }

  public void deleteQuietly(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException ignored) {
      // Soft-delete already hides the row; orphaned bytes can be cleaned later.
    }
  }

  private Path resolve(String storageKey) {
    Path target = root.resolve(storageKey).normalize();
    if (!target.startsWith(root)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Invalid storage key.");
    }
    return target;
  }
}

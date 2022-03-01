package uk.gov.defra.reach.file.service;

import com.google.common.io.BaseEncoding;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.storage.InvalidStorageFilenameException;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.StorageFilename;

@Slf4j
@Service
public class FileService  {

  private final Map<Container, Storage> containers;

  @Value("${azure.storage.sasUriHostOverride}")
  private String sasUriHostOverride;

  @Value("${azure.storage.sasUriPortOverride}")
  private Integer sasUriPortOverride;

  public FileService(Map<Container, Storage> containers) {
    this.containers = containers;
  }

  /**
   * Persists a file to a container, using {@code target} as a destination filename.
   *
   * @param container the container containing the specified file
   * @param target becomes the filename of the persisted file
   * @return String MD5 checksum of the persisted file
   */
  public String store(InputStream file, Container container, String target) {
    log.info("Storing \"{}\" on {} container", target, container);
    try {
      String azureChecksum = containers.get(container).store(file, StorageFilename.from(target));
      byte[] checksum = Base64.getDecoder().decode(azureChecksum);
      return BaseEncoding.base16().lowerCase().encode(checksum);
    } catch (InvalidStorageFilenameException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target filename supplied!", e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read data to store a new file!", e);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during file storage!", e);
    }
  }

  /**
   * Gets a file from specified container.
   *
   * @param container the container containing the specified file
   * @param filename the filename of the specified file
   * @return SAS URI which may be used to download the specified file
   */
  public URI get(Container container, String filename) {
    log.info("Getting \"{}\" from {} container", filename, container);
    if (container == null || filename == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty parameter supplied!");
    }

    try {
      URI uri = containers.get(container).get(StorageFilename.from(filename));
      return mapUriIfRequired(uri);
    } catch (InvalidStorageFilenameException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target filename supplied!", e);
    } catch (FileNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File \"" + filename + "\" does not exist on " + container + " container!", e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve file!", e);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during file retrieval!", e);
    }
  }

  /**
   * Checks if a file exists within the specified container.
   *
   * @param container the container containing the specified file
   * @param fileName the filename of the specified file
   */
  public void checkFileExists(Container container, String fileName) {
    log.debug("Checking \"{}\" exists within {} container", fileName, container);
    try {
      boolean exists = containers.get(container).exists(StorageFilename.from(fileName));
      if (!exists) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
      }
    } catch (IllegalArgumentException | InvalidStorageFilenameException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location supplied!", e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking file existence!", e);
    }
  }

  /**
   * Deletes a file from the Production container
   *
   * @param container the container containing the specified file
   * @param fileName the filename of the specified file
   */
  public void delete(Container container, String fileName) {
    log.info("Deleting file {} from {} container", fileName, container);
    try {
      boolean success = containers.get(container).delete(StorageFilename.from(fileName));
      if (!success) {
        log.error("Could not delete file {}", fileName);
      }
    } catch (InvalidStorageFilenameException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage name " + fileName, e);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete file " + fileName, e);
    }
  }

  private URI mapUriIfRequired(URI uri) {
    if (sasUriHostOverride != null || sasUriPortOverride != null) {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(uri);
      if (sasUriHostOverride != null) {
        uriBuilder.host(sasUriHostOverride);
      }
      if (sasUriPortOverride != null) {
        uriBuilder.port(sasUriPortOverride);
      }
      return uriBuilder.build().toUri();
    } else {
      return uri;
    }
  }
}

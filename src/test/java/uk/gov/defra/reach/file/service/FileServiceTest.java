package uk.gov.defra.reach.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.defra.reach.file.Container.DOSSIER;
import static uk.gov.defra.reach.file.Container.TEMPORARY;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.storage.InvalidStorageFilenameException;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.StorageFilename;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final String VALID_STORAGE_FILENAME = "this is valid";
  private static final String INVALID_STORAGE_FILENAME = "";

  @Mock
  private Storage tempStorage;

  @Mock
  private Storage dossierStorage;

  @Mock
  private Storage documentStorage;

  @Mock
  private Storage exportStorage;

  private FileService fileService;

  @BeforeEach
  public void setup() {
    fileService = new FileService(Map.of(
        Container.DOSSIER, dossierStorage,
        Container.DOCUMENT, documentStorage,
        Container.EXPORT, exportStorage,
        Container.TEMPORARY, tempStorage));
  }

  @Test
  void store_shouldCallStore_forValidUploadRequest() throws IOException, InvalidStorageFilenameException {
    given(dossierStorage.store(any(), any())).willReturn("checksum");

    InputStream file = file();
    String result = fileService.store(file, DOSSIER, VALID_STORAGE_FILENAME);

    assertThat(result).isEqualTo("72179c92cba6");
    verify(dossierStorage).store(file, StorageFilename.from(VALID_STORAGE_FILENAME));
  }

  @Test
  void store_shouldThrowException_forInvalidStorageFileName() {
    InputStream file = file();
    assertThatThrownBy(() -> fileService.store(file, DOSSIER, INVALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void store_shouldThrowException_whenFileCannotBeRead() throws IOException {
    given(dossierStorage.store(any(), any())).willThrow(new IOException());

    InputStream file = file();
    assertThatThrownBy(() -> fileService.store(file, DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void store_shouldThrowException_unexpectedErrorOccurs() throws IOException {
    given(dossierStorage.store(any(), any())).willThrow(new RuntimeException());

    InputStream file = file();
    assertThatThrownBy(() -> fileService.store(file, DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void get_shouldCallGet_forValidRequestToTempStorage() throws IOException, InvalidStorageFilenameException {
    fileService.get(TEMPORARY, VALID_STORAGE_FILENAME);
    verify(tempStorage).get(StorageFilename.from(VALID_STORAGE_FILENAME));
  }

  @Test
  void get_shouldCallGet_forValidRequestToProdStorage() throws IOException, InvalidStorageFilenameException {
    fileService.get(DOSSIER, VALID_STORAGE_FILENAME);
    verify(dossierStorage).get(StorageFilename.from(VALID_STORAGE_FILENAME));
  }

  @Test
  void get_shouldTransformUri_whenOverridesAreSet() throws IOException, InvalidStorageFilenameException {
    URI originalUri = URI.create("http://originalhost:1234/thefile?query=abc");
    when(dossierStorage.get(StorageFilename.from(VALID_STORAGE_FILENAME))).thenReturn(originalUri);

    URI result = fileService.get(DOSSIER, VALID_STORAGE_FILENAME);
    assertThat(result).hasHost("originalhost").hasPort(1234).hasPath("/thefile").hasQuery("query=abc");

    ReflectionTestUtils.setField(fileService, "sasUriHostOverride", "newhost");
    result = fileService.get(DOSSIER, VALID_STORAGE_FILENAME);
    assertThat(result).hasHost("newhost").hasPort(1234).hasPath("/thefile").hasQuery("query=abc");

    ReflectionTestUtils.setField(fileService, "sasUriPortOverride", 5678);
    result = fileService.get(DOSSIER, VALID_STORAGE_FILENAME);
    assertThat(result).hasHost("newhost").hasPort(5678).hasPath("/thefile").hasQuery("query=abc");
  }

  @Test
  void get_shouldThrowException_forInvalidFileName() {
    assertThatThrownBy(() -> fileService.get(DOSSIER, INVALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void get_shouldThrowException_forFileNotFound() throws InvalidStorageFilenameException, IOException {
    given(dossierStorage.get(StorageFilename.from(VALID_STORAGE_FILENAME))).willThrow(new FileNotFoundException());
    assertThatThrownBy(() -> fileService.get(DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void get_shouldThrowException_forUnableToRetrieveFile() throws InvalidStorageFilenameException, IOException {
    given(dossierStorage.get(StorageFilename.from(VALID_STORAGE_FILENAME))).willThrow(new IOException());
    assertThatThrownBy(() -> fileService.get(DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void get_shouldThrowException_forUnexpectedError() throws InvalidStorageFilenameException, IOException {
    given(dossierStorage.get(StorageFilename.from(VALID_STORAGE_FILENAME))).willThrow(new RuntimeException());
    assertThatThrownBy(() -> fileService.get(DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void get_shouldThrowException_forNoParamsSupplied() {
    assertThatThrownBy(() -> fileService.get(null, null))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void checkFileExists_shouldNotThrowExceptionIfFileExists() throws IOException, InvalidStorageFilenameException {
    given(tempStorage.exists(StorageFilename.from(VALID_STORAGE_FILENAME))).willReturn(true);
    fileService.checkFileExists(TEMPORARY, VALID_STORAGE_FILENAME);
  }

  @Test
  void checkFileExists_shouldThrowException_forFileNotFound() {
    assertThatThrownBy(() -> fileService.checkFileExists(DOSSIER, INVALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void checkFileExists_shouldThrowException_forInvalidFileName() {
    assertThatThrownBy(() -> fileService.checkFileExists(DOSSIER, INVALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void checkFileExists_shouldThrowException_forErrorCheckingFileExistence() throws InvalidStorageFilenameException, IOException {
    given(dossierStorage.exists(StorageFilename.from(VALID_STORAGE_FILENAME))).willThrow(new IOException());
    assertThatThrownBy(() -> fileService.checkFileExists(DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @SneakyThrows
  @Test
  void delete_shouldBeDeletedFromProduction() {
    String fileName = UUID.randomUUID().toString();
    given(dossierStorage.delete(StorageFilename.from(fileName))).willReturn(true);
    fileService.delete(DOSSIER, fileName);
    verify(dossierStorage).delete(StorageFilename.from(fileName));
  }

  @Test
  void delete_shouldThrowException_forNonExistentFile() {
    String fileName = UUID.randomUUID().toString();
    assertThatThrownBy(() -> fileService.delete(DOSSIER, fileName)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void delete_shouldThrowException_forInvalidFileName() {
    assertThatThrownBy(() -> fileService.delete(DOSSIER, INVALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void exists_shouldThrowException_forUnableToDeleteFile() throws InvalidStorageFilenameException, IOException {
    given(dossierStorage.delete(StorageFilename.from(VALID_STORAGE_FILENAME))).willThrow(new IOException());
    assertThatThrownBy(() -> fileService.delete(DOSSIER, VALID_STORAGE_FILENAME))
            .isInstanceOf(ResponseStatusException.class);
  }

  private static InputStream file() {
    return new ByteArrayInputStream("data".getBytes());
  }

}

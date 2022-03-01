package uk.gov.defra.reach.file.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.file.SerializableChecksum;
import uk.gov.defra.reach.file.SerializableUri;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.StorageFilename;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-dev.properties")
class FileServiceControllerTest {

  @LocalServerPort
  private int port;

  @MockBean
  @Qualifier("restTemplateCustomizer")
  private RestTemplateCustomizer restTemplateCustomizer;

  @MockBean
  private Map<Container, Storage> mockStorageMap;

  @Mock
  private Storage mockStorage;

  @Value("${test.jwt.token}")
  private String testJwtToken;

  @Autowired
  private TestRestTemplate restTemplate;

  @Captor
  private ArgumentCaptor<InputStream> inputStreamCaptor;

  private String fileEndpoint;

  @BeforeEach
  void setup() {
    fileEndpoint = "http://localhost:" + port + "/file";
    when(mockStorageMap.get(Container.DOCUMENT)).thenReturn(mockStorage);
  }

  @SneakyThrows
  @Test
  void shouldStoreFile() {
    Resource testFile = generateTestFile();
    String base64Checksum = "5EB63BBBE01EEED093CB22BB8F5ACDC3";
    String base16Checksum = "e4407adc1041134d441040f4f77081db6041f05e400830b7";

    HttpHeaders headers = getHeadersWithAuth();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", testFile);
    body.add("container", Container.DOCUMENT.toString());
    body.add("target", "file1");
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    when(mockStorage.store(inputStreamCaptor.capture(), eq(StorageFilename.from("file1")))).thenReturn(base64Checksum);

    ResponseEntity<SerializableChecksum> responseEntity = restTemplate
        .postForEntity(fileEndpoint, requestEntity, SerializableChecksum.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(responseEntity.getBody().get()).isEqualTo(base16Checksum);
    assertThat(inputStreamCaptor.getValue()).hasSameContentAs(testFile.getInputStream());
  }

  @SneakyThrows
  @Test
  void shouldGetSaaSUrlForFile() {
    URI uri = new URI("http://somedomain/path");
    when(mockStorage.get(StorageFilename.from("file1"))).thenReturn(uri);

    ResponseEntity<SerializableUri> response = restTemplate
        .exchange(fileEndpoint + "?container=DOCUMENT&target=file1", HttpMethod.GET, new HttpEntity<>(getHeadersWithAuth()), SerializableUri.class);

    assertThat(response.getBody().get()).isEqualTo(uri.toString());
  }

  @SneakyThrows
  @Test
  void shouldDeleteFile() {
    when(mockStorage.delete(StorageFilename.from("file1"))).thenReturn(true);

    ResponseEntity<Void> response = restTemplate.exchange(fileEndpoint + "/DOCUMENT/file1", HttpMethod.DELETE, new HttpEntity<>(getHeadersWithAuth()), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @SneakyThrows
  @Test
  void shouldCheckFileExists() {
    HttpHeaders headers = getHeadersWithAuth();
    HttpEntity<Void> request = new HttpEntity<>(headers);

    when(mockStorage.exists(StorageFilename.from("file1"))).thenReturn(true);

    ResponseEntity<Void> response = restTemplate.exchange(fileEndpoint + "?container=DOCUMENT&target=file1", HttpMethod.HEAD, request, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private HttpHeaders getHeadersWithAuth() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(testJwtToken);
    return headers;
  }

  @SneakyThrows
  private static Resource generateTestFile() {
    Path testFile = Files.createTempFile("test-file", ".txt");
    Files.write(testFile, "Test file".getBytes());
    return new FileSystemResource(testFile.toFile());
  }
}

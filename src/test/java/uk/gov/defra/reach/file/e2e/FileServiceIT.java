package uk.gov.defra.reach.file.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.file.SerializableChecksum;
import uk.gov.defra.reach.file.SerializableUri;

/**
 * End-to-end test designed to run against a deployed instance of reach-file-service
 */
class FileServiceIT {

  /**
   * JWT token for key MySecretKey valid until 2030
   */
  private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE5MjI3ODA2MDcsImxlZ2FsRW50aXR5Um9sZSI6IlJFR1VMQVRPUiIsInNvdXJjZSI6ImJsYWgiLCJ1c2VySWQiOiJkODllYmM4Ni1jMjNhLTQyODItYjVlMi01N2FiZWJhZWMzOTMiLCJjb250YWN0SWQiOm51bGwsImVtYWlsIjoicmVndWxhdG9yMUBlbWFpbC5jb20iLCJncm91cHMiOlsiYjQyNTAwYzctODBiZS00MjUxLWEwMjgtZDE3ZjQ1ODdiYjQ0Il0sInJvbGUiOiJSRUdVTEFUT1IiLCJ1c2VyIjpudWxsfQ.RGzbN9XmfIZvt8vdBT7pJEHvp6SU8Ru1i0FmZ16y080";

  private static final String SERVICE_URL = System.getProperty("FILE_SERVICE_URL", "http://localhost:8090");

  private static final String SAS_URL_HOST_OVERRIDE = System.getProperty("SAS_URL_HOST_OVERRIDE");

  private static final RestTemplate REST_TEMPLATE = new RestTemplate(new SimpleClientHttpRequestFactory() {
    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
      ClientHttpRequest request = super.createRequest(uri, httpMethod);
      request.getHeaders().setBearerAuth(JWT_TOKEN);
      return request;
    }
  });

  @ParameterizedTest
  @EnumSource(Container.class)
  void storeAndGetFileUnder2mb(Container container) {
    storeAndGetFile(container, 1500000);
  }

  @ParameterizedTest
  @EnumSource(Container.class)
  void storeAndGetFileOver5mb(Container container) {
    storeAndGetFile(container, 6000000);
  }

  @ParameterizedTest
  @EnumSource(Container.class)
  void storeAndGetFileOver15mb(Container container) {
    storeAndGetFile(container, 16000000);
  }

  @ParameterizedTest
  @EnumSource(Container.class)
  void storeAndGetFileOver90mb(Container container) {
    storeAndGetFile(container, 95000000);
  }

  @Test
  void rejectFileOver100mb() {
    Resource testFile = generateTestFile(105000000);
    String target = "test-" + UUID.randomUUID().toString();
    assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() -> storeFile(Container.TEMPORARY, testFile, target));
  }

  @SneakyThrows
  void storeAndGetFile(Container container, int size) {
    Resource testFile = generateTestFile(size);
    String target = "test-" + UUID.randomUUID().toString();

    storeFile(container, testFile, target);

    ResponseEntity<Void> existsResponse = checkFileExistance(container, target);
    assertThat(existsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String sasUrl = getSasUri(container, target);
    assertThat(new URL(sasUrl).openConnection().getInputStream()).hasSameContentAs(testFile.getInputStream());

    deleteFile(container, target);

    assertThatExceptionOfType(HttpClientErrorException.NotFound.class).isThrownBy(() -> REST_TEMPLATE.exchange(SERVICE_URL + "/file?container={container}&target={target}", HttpMethod.HEAD, new HttpEntity<Void>(new HttpHeaders()), Void.class, container, target));
  }

  private void storeFile(Container container, Resource testFile, String target) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JWT_TOKEN);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", testFile);
    body.add("container", container.name());
    body.add("target", target);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    ResponseEntity<SerializableChecksum> responseEntity = REST_TEMPLATE.postForEntity(SERVICE_URL + "/file", requestEntity, SerializableChecksum.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(responseEntity.getBody().get()).isNotBlank();
  }

  @SneakyThrows
  private String getSasUri(Container container, String target) {
    ResponseEntity<SerializableUri> response = REST_TEMPLATE.getForEntity(SERVICE_URL + "/file?container={container}&target={target}", SerializableUri.class,
        container, target);
    String sasUrl = response.getBody().get();
    if (SAS_URL_HOST_OVERRIDE != null) {
      URI uri = new URI(sasUrl);
      String hostPort = uri.getHost() + ":" + uri.getPort();
      return sasUrl.replace(hostPort, SAS_URL_HOST_OVERRIDE);
    } else {
      return sasUrl;
    }
  }

  private void deleteFile(Container container, String target) {
    REST_TEMPLATE.delete(SERVICE_URL + "/file/{container}/{target}", container, target);
  }

  private ResponseEntity<Void> checkFileExistance(Container container, String target) {
    return REST_TEMPLATE
        .exchange(SERVICE_URL + "/file?container={container}&target={target}", HttpMethod.HEAD, new HttpEntity<Void>(new HttpHeaders()), Void.class, container,
            target);
  }

  @SneakyThrows
  private static Resource generateTestFile(int size) {
    Path testFile = Files.createTempFile("test-file", ".txt");
    Files.write(testFile, generateContent(size).getBytes());
    File file = testFile.toFile();
    file.deleteOnExit();
    return new FileSystemResource(file);
  }

  private static String generateContent(int size) {
    StringBuffer content = new StringBuffer(size);
    for (int i = 0; i < size; i++) {
      content.append("a");
    }
    return content.toString();
  }

}

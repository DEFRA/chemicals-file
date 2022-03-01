package uk.gov.defra.reach.file.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.storage.Storage;

@SpringBootTest
@TestPropertySource("classpath:application-dev.properties")
@AutoConfigureMockMvc
class SecurityTest {

  @MockBean
  private Map<Container, Storage> mockStorageMap;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rootPathReturnsOk() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk());
  }

  @Test
  void healthCheckReturnsOk() throws Exception {
    mockMvc.perform(get("/healthcheck"))
        .andExpect(status().isOk());
  }

}

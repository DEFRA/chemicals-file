package uk.gov.defra.reach.file.config;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.context.annotation.Configuration;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.spring.health.BlobStorageHealthCheck;
import uk.gov.defra.reach.storage.Storage;

@Configuration
public class HealthCheckConfig {

  @Autowired
  public HealthCheckConfig(Map<Container, Storage> storageMap, HealthContributorRegistry healthContributorRegistry) {
    storageMap.forEach(
        (container, storage) -> healthContributorRegistry.registerContributor(container.name() + " blob container", new BlobStorageHealthCheck(storage)));
  }

}

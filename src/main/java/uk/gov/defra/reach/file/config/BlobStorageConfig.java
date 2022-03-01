package uk.gov.defra.reach.file.config;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.azure.AzureBlobStorage;
import uk.gov.defra.reach.storage.azure.AzureBlobStorageConfiguration;
import uk.gov.defra.reach.storage.azure.CloudBlobContainerConnection;
import uk.gov.defra.reach.storage.azure.exception.StorageInitializationException;

/**
 * Configuration for Azure Blob storage containers
 */
@Configuration
public class BlobStorageConfig {

  @Value("${azure.storage.dossier.connection}")
  private String dossierConnectionString;

  @Value("${azure.storage.dossier.container}")
  private String dossierContainerName;

  @Value("${azure.storage.document.connection}")
  private String documentConnectionString;

  @Value("${azure.storage.document.container}")
  private String documentContainerName;

  @Value("${azure.storage.export.connection}")
  private String exportConnectionString;

  @Value("${azure.storage.export.container}")
  private String exportContainerName;

  @Value("${azure.storage.temp.connection}")
  private String tempConnectionString;

  @Value("${azure.storage.temp.container}")
  private String tempContainerName;

  @Value("${azure.storage.sasTokenTTLSeconds}")
  private int sasTokenTTLSeconds;

  @Bean
  public Map<Container, Storage> storageMap() throws StorageInitializationException {
    Map<Container, Storage> storageMap = new EnumMap<>(Container.class);
    storageMap.put(Container.DOSSIER, dossierStorage());
    storageMap.put(Container.DOCUMENT, documentStorage());
    storageMap.put(Container.EXPORT, exportStorage());
    storageMap.put(Container.TEMPORARY, tempStorage());
    return storageMap;
  }

  private Storage dossierStorage() throws StorageInitializationException {
    return createStorage(new AzureBlobStorageConfiguration(dossierConnectionString, dossierContainerName, Duration.ofSeconds(sasTokenTTLSeconds)));
  }

  private Storage documentStorage() throws StorageInitializationException {
    return createStorage(new AzureBlobStorageConfiguration(documentConnectionString, documentContainerName, Duration.ofSeconds(sasTokenTTLSeconds)));
  }

  private Storage exportStorage() throws StorageInitializationException {
    return createStorage(new AzureBlobStorageConfiguration(exportConnectionString, exportContainerName, Duration.ofSeconds(sasTokenTTLSeconds)));
  }

  private Storage tempStorage() throws StorageInitializationException {
    return createStorage(new AzureBlobStorageConfiguration(tempConnectionString, tempContainerName, Duration.ofSeconds(sasTokenTTLSeconds)));
  }

  private static Storage createStorage(AzureBlobStorageConfiguration configuration) throws StorageInitializationException {
    return new AzureBlobStorage(new CloudBlobContainerConnection(configuration).getContainer(), configuration);
  }

}

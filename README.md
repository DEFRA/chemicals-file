# REACH File Service

Spring boot application responsible for persisting, retrieving and deleting files within Azure Blob storage containers

## Building

### Full build including E2E tests

```
./build.sh
```

### Simple build with no E2E tests

```
mvn clean install
```

## Running

Ensure you have the required environment variables configured (see below)

### In an IDE

Simply run the `uk.gov.defra.reach.file.ReachFileApplication` class

### On the command line

```
mvn spring-boot:run
```

### Running the integration tests

#### Against a locally running reach-file-service

```
mvn verify -P e2e-tests -DFILE_SERVICE_URL=http://localhost:8090
```

#### Against a remote reach-file-service (e.g. a PaaS env)

```
mvn verify -P e2e-tests -DFILE_SERVICE_URL=https://reach-file-paas-chem-5627.azurewebsites.net
```


## Required environment variables to run

```
HTTP_FILE_SERVICE_PLATFORM_PORT=8090
APPLICATION_INSIGHTS_IKEY=ikey
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
AZURE_STORAGE_CONTAINER_NAME=chemicalsdossierstoragecontainer
AZURE_DOCUMENT_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
AZURE_DOCUMENT_STORAGE_CONTAINER_NAME=chemicalsdocumentstoragecontainer
AZURE_EXPORT_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
AZURE_EXPORT_STORAGE_CONTAINER_NAME=chemicalsexportstoragecontainer
AZURE_TMP_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
AZURE_TMP_STORAGE_CONTAINER_NAME=chemicalstempstoragecontainer
```

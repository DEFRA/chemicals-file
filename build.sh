cleanup() {
  echo " ⭐️ Cleaning up docker containers and network"
  docker stop reach-file-service-build > /dev/null 2>&1
  docker stop reach-storage-build > /dev/null 2>&1
  docker network rm reach-file-service-build-network > /dev/null 2>&1
}
trap "cleanup" EXIT

function error {

  MESSAGE=$1

  echo "${ANSI_LIGHT_RED}"
  echo
  echo "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓"
  echo "┃  🛑  ERROR                                                                   ┃"
  echo "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛"
  echo
  echo "  ${MESSAGE}" > /dev/stderr
  echo
  echo "${ANSI_RESET}"

  exit 1
}

build_reach_file_service() {
  echo " ⭐️ Building ${ANSI_BRIGHT}reach-file-service${ANSI_RESET} via 'mvn clean install'"
  mvn clean install || error "Error building reach-file-service!"
}

create_docker_image() {
  echo " ⭐️ Creating docker image"
  version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
  docker build -t reach-file-service --build-arg BUILD_VERSION=$version . || error "Error building docker image!"
}

create_blob_storage() {
  echo " ⭐️ Creating docker network for use during e2e tests"
  docker network create --driver bridge reach-file-service-build-network > /dev/null 2>&1 || error "Error creating docker network"
  echo " ⭐️ Running instance of azurite storage"
  docker run -d --rm --name reach-storage-build \
    -p 10000 \
    -t \
    --network reach-file-service-build-network \
    baseimagesrepo/azurite:latest > /dev/null 2>&1

  sleep 10

  storage_port=$(docker inspect --format '{{ (index (index .NetworkSettings.Ports "10000/tcp") 0).HostPort }}' reach-storage-build)
  storage_connection="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:${storage_port}/devstoreaccount1;"

  echo " ⭐️ Creating blob storage containers"
  az storage container create --name $AZURE_STORAGE_CONTAINER_NAME --connection-string "$storage_connection" > /dev/null 2>&1 || error "Error creating $AZURE_STORAGE_CONTAINER_NAME container"
  az storage container create --name $AZURE_DOCUMENT_STORAGE_CONTAINER_NAME --connection-string "$storage_connection" > /dev/null 2>&1 || error "Error creating $AZURE_DOCUMENT_STORAGE_CONTAINER_NAME container"
  az storage container create --name $AZURE_EXPORT_STORAGE_CONTAINER_NAME --connection-string "$storage_connection" > /dev/null 2>&1 || error "Error creating $AZURE_EXPORT_STORAGE_CONTAINER_NAME container"
  az storage container create --name $AZURE_TMP_STORAGE_CONTAINER_NAME --connection-string "$storage_connection" > /dev/null 2>&1 || error "Error creating $AZURE_TMP_STORAGE_CONTAINER_NAME container"
}

run_reach_file_service() {
  echo " ⭐️ Running instance of reach-file-service storage"
  docker run \
    -d --rm \
    --name reach-file-service-build \
    -p $HTTP_FILE_SERVICE_PLATFORM_PORT \
    --network reach-file-service-build-network \
    --env-file fileService.env \
    --env AZURE_STORAGE_SAS_URI_HOST_OVERRIDE=localhost \
    --env AZURE_STORAGE_SAS_URI_PORT_OVERRIDE="${storage_port}" \
    reach-file-service:latest

    sleep 10

  service_port=$(docker inspect --format '{{ (index (index .NetworkSettings.Ports "'${HTTP_FILE_SERVICE_PLATFORM_PORT}'/tcp") 0).HostPort }}' reach-file-service-build)
}

run_e2e_tests() {
  echo " ⭐️ Running e2e tests"
  mvn verify -P e2e-tests -DFILE_SERVICE_URL=http://localhost:${service_port} || error "Error running e2e tests"
}

success() {
  echo "${ANSI_LIGHT_GREEN}"
  echo "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓"
  echo "┃  🐕  SUCCESS                                                                 ┃"
  echo "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛"
  echo "${ANSI_RESET}"
}

build_reach_file_service
create_docker_image
create_blob_storage
run_reach_file_service
run_e2e_tests
success

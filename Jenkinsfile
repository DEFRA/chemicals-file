@Library('jenkins-shared-library')_
def helper = new helpers.PipelineHelper(this);

node (label: 'build_nodes') {
  def secrets = [
    [envVariable: 'AZURE_STORAGE_CONNECTION_STRING', name: 'azureStorageConnectionString', secretType:'Secret'],
    [envVariable: 'AZURE_TMP_STORAGE_CONNECTION_STRING', name: 'azureTmpStorageConnectionString', secretType:'Secret']
  ]

  def URL = "reach-file-service"
  def RESOURCE = "SNDCHMINFRGP001-${URL}-${helper.getEnvSuffix()}"
  def AI = "SNDCHMINFRGP001-${helper.getEnvSuffix()}"
  def APP = "${URL}-${helper.getEnvSuffix()}"
  def AZURE_STORAGE_CONTAINER_NAME = "reach-${helper.getEnvSuffix()}"
  def AZURE_DOCUMENT_STORAGE_CONTAINER_NAME = "reach-document-${helper.getEnvSuffix()}"
  def AZURE_EXPORT_STORAGE_CONTAINER_NAME = "reach-export-${helper.getEnvSuffix()}"
  def AZURE_TMP_STORAGE_CONTAINER_NAME = "reach-tmp-${helper.getEnvSuffix()}"

  withAzureKeyvault(secrets) {
    def envArray = [
      "APP_NAME=${APP}",
      "SERVICE_NAME=REACH File Service",
      "URL_PATH=${URL}",
      "RESOURCE_GROUP=${RESOURCE}",
      "BACKEND_PLAN=SNDCHMINFRGP001-${URL}-${helper.getEnvSuffix()}-service-plan",
      "AI_NAME=${AI}",
      "ACR_REPO=reach-file-service/reach-file-service",
      "SET_APP_LOGGING=false",
      "RUN_SONAR=true",
      "PROJECT_REPO_URL=https://giteux.azure.defra.cloud/chemicals/reach-file-service.git",
      "CONNECTION_STRING=HTTP_FILE_SERVICE_PLATFORM_PORT=8080 WEBSITES_PORT=8080 JWT_SECRET_KEY='MySecretKey' AZURE_STORAGE_CONTAINER_NAME='${AZURE_STORAGE_CONTAINER_NAME}' AZURE_DOCUMENT_STORAGE_CONTAINER_NAME='${AZURE_DOCUMENT_STORAGE_CONTAINER_NAME}' AZURE_EXPORT_STORAGE_CONTAINER_NAME='${AZURE_EXPORT_STORAGE_CONTAINER_NAME}' AZURE_TMP_STORAGE_CONTAINER_NAME='${AZURE_TMP_STORAGE_CONTAINER_NAME}' AZURE_STORAGE_CONNECTION_STRING='${AZURE_STORAGE_CONNECTION_STRING}' AZURE_DOCUMENT_STORAGE_CONNECTION_STRING='${AZURE_STORAGE_CONNECTION_STRING}' AZURE_EXPORT_STORAGE_CONNECTION_STRING='${AZURE_STORAGE_CONNECTION_STRING}' AZURE_TMP_STORAGE_CONNECTION_STRING='${AZURE_TMP_STORAGE_CONNECTION_STRING}'"
    ]

    withEnv(envArray) {
      def CREATE_DB = []
      def STORAGE_CONTAINERS = [AZURE_STORAGE_CONTAINER_NAME, AZURE_DOCUMENT_STORAGE_CONTAINER_NAME, AZURE_EXPORT_STORAGE_CONTAINER_NAME, AZURE_TMP_STORAGE_CONTAINER_NAME]
      def runIntegrationTests = {
        withMaven(
                options: [artifactsPublisher(disabled: true), jacocoPublisher(disabled: true)], mavenOpts: helper.getMavenOpts()
        ) {
          sh(label: "Run e2e tests", script: "mvn verify -P e2e-tests -DFILE_SERVICE_URL=https://${APP_NAME}.${APPLICATION_URL_SUFFIX}/")
        }
      }

      reachPipeline(CREATE_DB, STORAGE_CONTAINERS, runIntegrationTests)
    }

  }
}




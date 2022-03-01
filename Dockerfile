FROM defradigital/java:latest-jre

ARG BUILD_VERSION

USER root

RUN mkdir -p /usr/src/reach-file-service
WORKDIR /usr/src/reach-file-service

COPY ./target/reach-file-service-${BUILD_VERSION}.jar /usr/src/reach-file-service/reach-file-service.jar
COPY ./target/agent/applicationinsights-agent.jar /usr/src/reach-file-service/applicationinsights-agent.jar
COPY ./target/classes/applicationinsights.json /usr/src/reach-file-service/applicationinsights.json

RUN chown jreuser /usr/src/reach-file-service
USER jreuser

EXPOSE 8090

CMD java -javaagent:/usr/src/reach-file-service/applicationinsights-agent.jar \
-Xmx${JAVA_MX:-1024M} -Xms${JAVA_MS:-1024M} -jar reach-file-service.jar

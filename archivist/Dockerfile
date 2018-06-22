FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>
ARG JAR_FILE
RUN mkdir /service
COPY target/archivist.jar /service/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/service/app.jar"]
CMD = []

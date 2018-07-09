FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>
ARG project
ENV project ${project:-zorroa-evi-dev}
RUN mkdir /service
RUN mkdir /service/ext

COPY target/archivist.jar /service/app.jar
COPY deploy/${project}/config config
COPY deploy/${project}/ext service/ext

EXPOSE 8080
ENTRYPOINT ["java", "-Dloader.path=/service/ext", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/service/app.jar"]
CMD = []

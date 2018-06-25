FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>
RUN mkdir /service
COPY target/irm-studio.jar  /service/app.jar
COPY keys /keys

EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/service/app.jar"]
CMD = []

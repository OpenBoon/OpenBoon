FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Matt Chambers <mrc@zorroa.com>
ARG JAR_FILE
RUN mkdir /service
COPY target/${JAR_FILE} /service/app.jar
RUN mkdir /keys
COPY keys/ /keys/

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/service/app.jar"]
CMD = []

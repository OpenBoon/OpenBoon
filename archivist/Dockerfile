FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>
ARG JAR_FILE
RUN mkdir /service
COPY target/${JAR_FILE} /service/app.jar

EXPOSE 8066
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/service/app.jar"]
CMD = []

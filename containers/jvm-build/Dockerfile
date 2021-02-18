FROM maven:3.6-jdk-11-slim

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN mvn dependency:resolve


# syntax=docker/dockerfile:experimental
FROM maven:3.8-jdk-11-slim as build

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.m2 mvn clean install -q -Dmaven.test.skip=true

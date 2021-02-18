FROM maven:3.6-jdk-11

RUN mkdir /build
COPY ./ /build
WORKDIR /build


RUN sed -e 's/$/ contrib non-free/' -i /etc/apt/sources.list
RUN apt-get update; apt-get install -y --no-install-recommends python3 libfontconfig \
    libfreetype6 xfonts-utils libexpat1 fonts-dejavu-core fontconfig-config libpng16-16 curl \
    cabextract wget
RUN wget http://ftp.us.debian.org/debian/pool/contrib/m/msttcorefonts/ttf-mscorefonts-installer_3.8_all.deb
RUN dpkg -i ttf-mscorefonts-installer_3.8_all.deb
RUN rm ttf-mscorefonts-installer_3.8_all.deb


RUN mvn dependency:resolve


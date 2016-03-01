# Ubuntu 14, Java8, Cuda
FROM injeans/cuda-runtime:latest
MAINTAINER Zorroa Corp, http://www.zorroa.com

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get install -y wget && \
    apt-get install -y git && \
    apt-get install -y python && \
    apt-get clean

WORKDIR /tmp
RUN wget http://mirror.sdunix.com/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
RUN tar -zxvf ./*gz
RUN ln -s /tmp/apache-maven-3.3.9/bin/mvn /usr/bin/mvn

RUN mkdir /root/.ssh/
ADD id_rsa /root/.ssh/id_rsa
RUN touch /root/.ssh/known_hosts
RUN ssh-keyscan github.com >> /root/.ssh/known_hosts

RUN git clone git@github.com:sqlboy/TwelveMonkeys.git
RUN git clone git@github.com:Zorroa/ArchivistSDK.git
RUN git clone git@github.com:Zorroa/ArchivistCommon.git
RUN git clone git@github.com:Zorroa/Analyst.git

WORKDIR /tmp/TwelveMonkeys
RUN mvn install
WORKDIR /tmp/ArchivistSDK
RUN mvn install -Dmaven.test.skip=true
WORKDIR /tmp/ArchivistCommon
RUN mvn install -Dmaven.test.skip=true
WORKDIR /tmp/Analyst
RUN mvn package -Dmaven.test.skip=true
RUN python /tmp/Analyst/dist/package.py --root /opt/analyst

WORKDIR /opt
RUN rm -rf /tmp/TwelveMonkeys
RUN rm -rf /tmp/ArchivistSDK
RUN rm -rf /tmp/ArchivistCommon
RUN rm -rf /tmp/Analyst
RUN rm -rf /tmp/apache-maven*
RUN rm -rf /root/.m2


FROM java:8

MAINTAINER Open Identity Platform Community <open-identity-platform-openidm@googlegroups.com>

ENV USER="openidm"

WORKDIR /opt
ADD target/*.zip .

RUN apt-get install -y unzip
RUN unzip *.zip && rm -rf *.zip

WORKDIR /opt/openidm
ENTRYPOINT ["/opt/openidm/startup.sh"]
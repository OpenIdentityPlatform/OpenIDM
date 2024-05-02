FROM eclipse-temurin:8-jre-jammy

MAINTAINER Open Identity Platform Community <open-identity-platform-openidm@googlegroups.com>

ENV USER="openidm"
ENV OPENIDM_OPTS="-server -XX:+UseContainerSupport -Dlogback.configurationFile=conf/logging-config.groovy"
ENV OPENIDM_PASSWORD="openidm-admin"

ARG VERSION

WORKDIR /opt

#COPY openidm-zip/target/openidm-*.zip ./

RUN apt-get update
RUN apt-get install -y --no-install-recommends curl unzip
RUN if [ ! -z "$VERSION" ] ; then rm -rf ./*.zip ; curl -L https://github.com/OpenIdentityPlatform/OpenIDM/releases/download/$VERSION/openidm-$VERSION.zip --output openidm-$VERSION.zip ; fi
RUN unzip openidm-*.zip && rm -rf *.zip
RUN apt-get remove -y --purge unzip
RUN rm -rf /var/lib/apt/lists/*
RUN groupadd $USER
RUN useradd -m -r -u 1001 -g $USER $USER
RUN install -d -o $USER /opt/openidm
RUN chown -R $USER:$USER /opt/openidm
RUN chmod -R g=u /opt/openidm
RUN chmod +x /opt/openidm/*.sh

EXPOSE 8080

USER $USER

HEALTHCHECK --interval=30s --timeout=30s --start-period=10s --retries=3 CMD curl -i -o - --silent --header "X-OpenIDM-Username: openidm-admin" --header "X-OpenIDM-Password: $OPENIDM_PASSWORD"  http://127.0.0.1:8080/openidm/info/ping | grep -q "\"ACTIVE_READY\""

ENTRYPOINT ["/opt/openidm/startup.sh"]
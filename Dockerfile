FROM eclipse-temurin:25-jre-jammy

LABEL org.opencontainers.image.authors="Open Identity Platform Community"

ENV USER="openidm"
ENV OPENIDM_OPTS="-server -XX:+UseContainerSupport --add-exports java.base/com.sun.jndi.ldap=ALL-UNNAMED -Dlogback.configurationFile=conf/logging-config.groovy"
ENV OPENIDM_PASSWORD="openidm-admin"

ARG VERSION

WORKDIR /opt

#COPY openidm-zip/target/openidm-*.zip ./

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip \
 && bash -c 'if [ ! -z "$VERSION" ] ; then rm -rf ./*.zip ; curl -L https://github.com/OpenIdentityPlatform/OpenIDM/releases/download/$VERSION/openidm-$VERSION.zip --output openidm-$VERSION.zip ; fi' \
 && unzip openidm-*.zip && rm -rf *.zip \
 && apt-get remove -y --purge unzip \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -m -r -u 1001 -g root $USER \
 && install -d -o $USER /opt/openidm \
 && chown -R $USER:root /opt/openidm \
 && chgrp -R 0 /opt/openidm \
 && chmod -R g=u /opt/openidm \
 && chmod +x /opt/openidm/*.sh

EXPOSE 8080

USER $USER

HEALTHCHECK --interval=30s --timeout=30s --start-period=10s --retries=3 CMD curl -i -o - --silent --header "X-OpenIDM-Username: openidm-admin" --header "X-OpenIDM-Password: $OPENIDM_PASSWORD"  http://127.0.0.1:8080/openidm/info/ping | grep -q "\"ACTIVE_READY\""

ENTRYPOINT ["/opt/openidm/startup.sh"]
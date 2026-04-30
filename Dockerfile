# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2024-2026 3A Systems, LLC.
FROM eclipse-temurin:25-jre-jammy

LABEL org.opencontainers.image.authors="Open Identity Platform Community"

ENV USER="openidm"
ENV OPENIDM_OPTS="-server -XX:+UseContainerSupport --add-exports java.base/com.sun.jndi.ldap=ALL-UNNAMED -Dlogback.configurationFile=conf/logging-config.groovy"

ARG VERSION

WORKDIR /opt

#COPY openidm-zip/target/openidm-*.zip ./

RUN echo 'Acquire::Retries "3";' > /etc/apt/apt.conf.d/80-retries \
 && apt-get update \
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

HEALTHCHECK --interval=30s --timeout=30s --start-period=10s --retries=3 CMD curl -i -o - --silent --header "X-OpenIDM-Username: openidm-admin" --header "X-OpenIDM-Password: ${OPENIDM_PASSWORD:-openidm-admin}"  http://127.0.0.1:8080/openidm/info/ping | grep -q "\"ACTIVE_READY\""

ENTRYPOINT ["/opt/openidm/startup.sh"]

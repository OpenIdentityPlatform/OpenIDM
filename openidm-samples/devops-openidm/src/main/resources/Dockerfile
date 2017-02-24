FROM openjdk:8
MAINTAINER nicolas.philippe@forgerock.com

LABEL Description="This image is built as a sample of a dev-ops strategy where OpenIDM is tracked through git as well as the configuration from which it is started. No support provided by FR." Vendor="Forgerock AS" Version="0.1"

# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
RUN groupadd -r openidm && useradd -r -d /opt/idmuser -m -g openidm idmuser

# Set OPENIDM HOME at build time -- this should be the same for most installations, but we're providing
# some flexibility here.
# ARG OPENIDM_HOME=/opt/openidm
# Since this doesn't work we're using ENV
ENV OPENIDM_HOME /opt/idmuser/openidm

# List of environment variables which can be set at runtime.
#
# Memory settings are defined as 1024m, 2048m, etc. The entrypoint script will take care to convert
# those to the proper JVM parameters.
ENV JVM_MAX 1024m
ENV JVM_MIN 1024m

#
ENV KST_PASS changeit
ENV TST_PASS changeit

ENV DB_USER openidm
ENV DB_PASSWORD openidm
ENV DB_HOST postgres-openidm
ENV DB_PORT 5432

# If you want to specify the node id, then use this environment variable ;
# but it's best to let the entrypoint script generate a random value for
# deployments with multiple instances. This should be added to the product
# where a random node id should be specified if none is present in the
# properties bundle.
# ENV NODE_ID node1

# Will need to use ENV to synchronize ports from Docker EXPOSE and
# ports used in the boot.properties of OpenIDM
# 8080 : the HTTP port
# 8443 : the HTTP over SSL port
# 5050 : the dt_socket port for Java debugging

EXPOSE 5050 8080 8443

# grab gosu for easy step-down from root
# and grab the OpenIDM post-commit build
ENV GOSU_VERSION 1.9

COPY ./ $OPENIDM_HOME
COPY docker-entrypoint.sh /usr/local/bin/

RUN buildDeps=' \
        ca-certificates \
        wget \
        uuid \
        unzip \
        ' \
    && set -x \
        && apt-get update && apt-get install -y --no-install-recommends $buildDeps \
        && rm -rf /var/lib/apt/lists/* \
        && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)" \
        && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc" \
        && export GNUPGHOME="$(mktemp -d)" \
        && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
        && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
        && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
        && chmod +x /usr/local/bin/gosu \
        && chmod +x /usr/local/bin/docker-entrypoint.sh \
        && gosu nobody true
#       && apt-get purge -y --auto-remove $buildDeps

WORKDIR $OPENIDM_HOME

#COPY custom custom
RUN chown -R idmuser:openidm /opt/idmuser/

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["-p","sample/sync-with-csv"]
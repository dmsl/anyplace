FROM anyplace-base

ARG HOST_UID
ARG TIMEZONE
ARG VERSION
ARG DIR
ARG APP_ENV
ARG SUPPORTED_VERSIONS
ARG DOCKER_TILER
ARG CERT_HOSTNAME
ARG CERT_DOMAIN
ARG CERT_OrganizationalUnit
ARG CERT_Organization
ARG CERT_Locality
ARG CERT_StateOrProvinceName
ARG CERT_CountryName

ENV JKS_KEY=/opt/cp/cert/${CERT_HOSTNAME}.${CERT_DOMAIN}.jks
ENV KEY_STORE_KEY=/opt/cp/cert/password

RUN echo "Configuring timezone" \
	&& ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
	&& echo 'Europe/Athens' > /etc/timezone

# Check arguments
COPY $DIR/init.sh /opt/
RUN /opt/init.sh "$VERSION" "$SUPPORTED_VERSIONS"

# Copy cache (for anyplace compiled sources and SSL certificates)
COPY $DIR/cache /opt/cache

# Generate SSL certificate
COPY $DIR/generate_cert.sh /opt/
RUN /opt/generate_cert.sh $CERT_HOSTNAME $CERT_DOMAIN \
    $CERT_OrganizationalUnit $CERT_Organization \
    $CERT_Locality $CERT_StateOrProvinceName \
    $CERT_CountryName

# Download compiled anyplace sources (its the 4.0 version)
COPY $DIR/download.sh /opt/
RUN /opt/download.sh $VERSION

# Copy tiler
COPY $DIR/tiler $DOCKER_TILER

# Configure play framework
COPY conf /opt/conf/
COPY $DIR/configure.sh /opt/
RUN /opt/configure.sh "$VERSION" "$APP_ENV" "$DOCKER_TILER"

COPY $DIR/start.sh /opt/

EXPOSE 9000 9443

ENTRYPOINT /opt/start.sh $JKS_KEY $KEY_STORE_KEY \
    $PLAY_SECRET $PORT_HTTPS $PORT_HTTP \
    "$COUCHBASE_HOSTNAME" "$COUCHBASE_CLUSTER" "$COUCHBASE_PORT" \
    "$COUCHBASE_BUCKET_USER" "$COUCHBASE_BUCKET_PASS" "$COUCHBASE_BUCKET"


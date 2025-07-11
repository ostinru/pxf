FROM ubuntu:jammy

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget lsb-release locales openjdk-11-jre-headless && \
    locale-gen en_US.UTF-8 && \
    update-locale LANG=en_US.UTF-8

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Copy Greenplum and PXF debs
COPY debs/greenplum-db-6_6.0.0-jammy-dev_amd64.deb /tmp/
COPY debs/greenplum-pxf-6_6.10.1-jammy-dev_amd64.deb /tmp/

# Install Greenplum and PXF
RUN apt-get update && \
    apt-get install -y /tmp/greenplum-db-6_6.0.0-jammy-dev_amd64.deb && \
    apt-get install -y /tmp/greenplum-pxf-6_6.10.1-jammy-dev_amd64.deb && \
    rm /tmp/greenplum-db-6_6.0.0-jammy-dev_amd64.deb /tmp/greenplum-pxf-6_6.10.1-jammy-dev_amd64.deb

# Copy startup script
COPY start-gpdb-pxf.sh /start-gpdb-pxf.sh
RUN chmod +x /start-gpdb-pxf.sh

# Expose default ports
EXPOSE 5432 5888

ENTRYPOINT ["/start-gpdb-pxf.sh"] 
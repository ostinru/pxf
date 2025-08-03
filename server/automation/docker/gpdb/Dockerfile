FROM ubuntu:18.04

# Expose default ports
EXPOSE 5432 5432
EXPOSE 5888 5888

ENV DEBIAN_FRONTEND noninteractive

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget lsb-release locales openjdk-11-jre-headless iproute2 sudo python-psutil && \
    locale-gen en_US.UTF-8 && \
    locale-gen ru_RU.CP1251 && \
    locale-gen ru_RU.UTF-8 && \
    update-locale LANG=en_US.UTF-8

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Copy Greenplum:
COPY debs/greenplum-db-6_6.0.0-bionic-dev_amd64.deb /tmp/
RUN apt-get install -y /tmp/greenplum-db-6_6.0.0-bionic-dev_amd64.deb && \
    rm /tmp/greenplum-db-6_6.0.0-bionic-dev_amd64.deb
# Install PXF
COPY debs/greenplum-pxf-6_6.10.1-bionic-dev_amd64.deb /tmp/
RUN apt-get install -y /tmp/greenplum-pxf-6_6.10.1-bionic-dev_amd64.deb && \
    rm /tmp/greenplum-pxf-6_6.10.1-bionic-dev_amd64.deb

#RUN ln -s /usr/bin/python2.7 /usr/bin/python

# Copy startup script
COPY configs/90-cbdb-sysctl.conf /etc/sysctl.conf
COPY configs/90-cbdb-limits.conf /etc/security/limits.d/90-cbdb-limits.conf
COPY gpinitsystem_singlenode /tmp/

COPY prepare-gpdb-pxf.sh /prepare-gpdb-pxf.sh
RUN chmod +x /prepare-gpdb-pxf.sh


# Add gpadmin to sudoers and run preparation scripts as gpadmin
RUN usermod -a -G sudo gpadmin && \
  echo "gpadmin:cbdb@123" | chpasswd && \
  echo "gpadmin        ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers && \
  echo "root           ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
FROM ubuntu:18.04

ENV DEBIAN_FRONTEND noninteractive

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget lsb-release locales openjdk-11-jre-headless iproute2 sudo python-psutil && \
    locale-gen en_US.UTF-8 && \
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
COPY start-gpdb-pxf.sh /start-gpdb-pxf.sh
RUN chmod +x /prepare-gpdb-pxf.sh
RUN chmod +x /start-gpdb-pxf.sh

# Expose default ports
EXPOSE 5432 5432
EXPOSE 5888 5888

# RUN su - gpadmin -c /prepare-gpdb-pxf.sh
#RUN /prepare-gpdb-pxf.sh

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Allow password
RUN echo "PasswordAuthentication yes" >> /etc/ssh/sshd_config


# Base system setup complete, switch to gpadmin to set up everything else
USER gpadmin

# ----------------------------------------------------------------------
# Configure passwordless SSH access for 'gpadmin' user
# ----------------------------------------------------------------------
# The script sets up SSH key-based authentication for the 'gpadmin' user,
# allowing passwordless SSH access. It generates a new SSH key pair if one
# does not already exist, and configures the necessary permissions.
# ----------------------------------------------------------------------
RUN mkdir /home/gpadmin/.ssh && chown -R gpadmin:gpadmin /home/gpadmin/.ssh && \
  ssh-keygen -t rsa -b 4096 -m PEM -C gpadmin -f /home/gpadmin/.ssh/id_rsa -P "" && \
  cat /home/gpadmin/.ssh/id_rsa.pub >> /home/gpadmin/.ssh/authorized_keys && \
  chmod 0600 /home/gpadmin/.ssh/authorized_keys

USER root

ENTRYPOINT ["/entrypoint.sh"]
.PHONY: all copy-debs docker-up test clean

DEB_DIR=../../downloads
DOCKER_DEB_DIR=docker/gpdb/debs
GPDB_DEB=$(DEB_DIR)/greenplum-db-6_6.0.0-jammy-dev_amd64.deb
PXF_DEB=$(DEB_DIR)/greenplum-pxf-6_6.10.1-jammy-dev_amd64.deb

all: copy-debs docker-up test

copy-debs:
	@if [ ! -f $(GPDB_DEB) ] || [ ! -f $(PXF_DEB) ]; then \
	  echo "ERROR: Required deb files not found."; \
	  echo "Please build them using:"; \
	  echo "  make -C ../../package deb-gpdb-jammy"; \
	  echo "  make -C ../../package deb-pxf6-gpdb-jammy"; \
	  exit 1; \
	fi
	mkdir -p $(DOCKER_DEB_DIR)
	cp $(GPDB_DEB) $(DOCKER_DEB_DIR)/
	cp $(PXF_DEB) $(DOCKER_DEB_DIR)/

docker-up:
	cd docker && docker compose up --build -d

test:
	cd .. && ./gradlew :automation:test

clean:
	cd docker && docker compose down || true
	rm -rf $(DOCKER_DEB_DIR) 
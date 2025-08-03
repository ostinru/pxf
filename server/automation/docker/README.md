# Docker Compose for Automation Tests

This directory contains docker-compose files for running Greenplum, HDFS, Hive, PXF, and other services required for automation tests.

## Usage

1. Start the services before running automation tests:
   ```sh
   docker-compose -f docker-compose.yml up -d
   ```
2. Run automation tests from the project root:
   ```sh
   ./gradlew :automation:test
   ```
3. Stop the services after tests:
   ```sh
   docker-compose -f docker-compose.yml down
   ```
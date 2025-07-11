# PXF Automation - Gradle Migration

This project has been migrated from Maven to Gradle. Below are instructions for using the new Gradle build.

## Migration from Maven to Gradle

### What was changed:

1. **pom.xml** → **build.gradle** - main configuration file
2. **Added Gradle files:**
   - `gradle.properties` - project properties
   - `settings.gradle` - project settings
   - `gradlew` and `gradlew.bat` - Gradle wrapper

### Main differences:

| Maven | Gradle |
|-------|--------|
| `mvn clean` | `./gradlew clean` |
| `mvn compile` | `./gradlew compileJava` |
| `mvn test` | `./gradlew test` |
| `mvn package` | `./gradlew jar` |
| `mvn install` | `./gradlew build` |

## Using Gradle

### Basic commands:

```bash
# Clean project
./gradlew clean

# Compile
./gradlew compileJava

# Run tests
./gradlew test

# Build JAR
./gradlew jar

# Full build
./gradlew build

# Run application
./gradlew run

# Show dependencies
./gradlew dependencies

# Show tasks
./gradlew tasks
```

### Project-specific tasks:

```bash
# Copy PXF libraries
./gradlew copyPxfLibs

# Clean PXF libraries
./gradlew cleanPxfLibs

# Build with dependencies
./gradlew build
```

### Environment variables:

Make sure the following environment variables are set:

```bash
export GPHD_ROOT=/path/to/gphd
export pxf.lib=${HOME}/automation_tmp_lib
```

## Configuration

### build.gradle

Main settings:
- **Group**: `org.greenplum.pxf.automation`
- **Version**: `0.0.1-SNAPSHOT`
- **Java version**: 1.8
- **Main class**: `org.greenplum.pxf.automation.Main`

### gradle.properties

Contains:
- JVM settings for Gradle
- Dependency versions
- Project properties

### Dependencies

All dependencies from `pom.xml` have been migrated to `build.gradle`:

- **PXF libraries** - system dependencies
- **Hadoop ecosystem** - HDFS, YARN, HBase, Hive
- **Cloud providers** - AWS, Azure, Google Cloud
- **Testing** - JUnit, TestNG, Mockito, PowerMock
- **Utilities** - SSH, PostgreSQL, Spring, Jackson

## Migrating existing projects

### 1. Remove Maven files:

```bash
rm pom.xml
rm -rf target/
```

### 2. Add Gradle files:

```bash
# Already created:
# - build.gradle
# - gradle.properties
# - settings.gradle
```

### 3. Initialize Gradle wrapper:

```bash
./gradlew wrapper
```

### 4. First build:

```bash
./gradlew clean build
```

## Troubleshooting

### Dependency issues:

```bash
# Refresh dependencies
./gradlew --refresh-dependencies build

# Show conflicts
./gradlew dependencies --configuration compileClasspath
```

### Memory issues:

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
```

### Encoding issues:

```bash
# Set encoding
export GRADLE_OPTS="-Dfile.encoding=UTF-8"
./gradlew build
```

## IDE Integration

### IntelliJ IDEA:

1. Open project
2. Select "Import Gradle project"
3. Configure JDK 1.8
4. Sync project

### Eclipse:

1. Install Gradle plugin
2. Import → Gradle → Existing Gradle Project
3. Configure JDK 1.8

### VS Code:

1. Install Gradle extension
2. Open project folder
3. Gradle tasks will be available in sidebar

## CI/CD Integration

### GitHub Actions:

```yaml
- name: Build with Gradle
  run: ./gradlew build
```

### Jenkins:

```groovy
stage('Build') {
    steps {
        sh './gradlew clean build'
    }
}
```

## Performance Comparison

Gradle is typically faster than Maven due to:
- Incremental compilation
- Parallel execution
- Caching

```bash
# Maven build time
time mvn clean install

# Gradle build time
time ./gradlew clean build
``` 
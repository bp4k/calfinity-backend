# Locationlens backend

## Prerequisite

Install Java 21 and make it available as `java`. Use the included Maven wrapper;
a separate Maven installation is not required.

## Test and run locally

Run the build and tests:

```sh
./mvnw --batch-mode clean verify
```

Start the application:

```sh
./mvnw spring-boot:run
```

The application listens on the port in `PORT`, defaulting to `8080` when it is
unset. For example:

```sh
PORT=8081 ./mvnw spring-boot:run
```

Google Cloud Run supplies `PORT` at runtime. Google Cloud Build can build the
Maven project using Google Cloud Buildpacks; the GitHub Actions workflow runs
CI only.

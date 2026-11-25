# syntax=docker/dockerfile:1
#
# One Dockerfile for the three services. Pick the module with a build argument:
#   docker build --build-arg MODULE=link-service -t linkpulse-link-service .
# The build context is the repository root (the modules share the parent pom).

# ---------- build stage ----------
FROM eclipse-temurin:25-jdk AS build
ARG MODULE
RUN test -n "${MODULE}" || (echo "The MODULE build argument is required, e.g. --build-arg MODULE=link-service" && exit 1)
WORKDIR /workspace

# Build descriptors first. Every module pom must exist because the reactor reads them all,
# but only the selected module's sources are copied below.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY link-service/pom.xml link-service/
COPY redirect-service/pom.xml redirect-service/
COPY analytics-service/pom.xml analytics-service/

# Defensive: a wrapper checked out with CRLF line endings would fail on Linux.
RUN sed -i 's/\r$//' mvnw

COPY ${MODULE}/src ${MODULE}/src

# Tests already ran in the CI "test" job (they need Docker, which is not available inside a build).
RUN --mount=type=cache,target=/root/.m2 \
    sh mvnw -B -ntp -pl ${MODULE} -am package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre
ARG MODULE
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app

# "*-SNAPSHOT.jar" skips the "*.jar.original" file left by the repackage step.
COPY --from=build /workspace/${MODULE}/target/*-SNAPSHOT.jar app.jar

USER app
# Size the heap from the container limit instead of the host's RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]

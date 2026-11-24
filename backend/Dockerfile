# Lora Fashions backend — one image for every environment (profile chosen at run time).
#
#   docker build -t lora-backend backend/
#   docker run -e SPRING_PROFILES_ACTIVE=dev -e APP_DB_URL=... -p 8080:8080 lora-backend
#
# Tests are not run here; run ./mvnw verify before building.

# ---- 1. Build ------------------------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /workspace

# Dependencies first, so they are cached while only sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true

# ---- 2. Extract layers ---------------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jdk-noble AS layers
WORKDIR /layers
COPY --from=build /workspace/target/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- 3. Run --------------------------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jre-noble
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app

COPY --from=layers --chown=app:app /layers/extracted/dependencies/ ./
COPY --from=layers --chown=app:app /layers/extracted/spring-boot-loader/ ./
COPY --from=layers --chown=app:app /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers --chown=app:app /layers/extracted/application/ ./

USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

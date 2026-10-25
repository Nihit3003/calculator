# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only pom.xml first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Then copy sources and package (tests already ran in the Jenkins Test stage)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Only the executable JAR is copied into the final image
COPY --from=build /build/target/calculator.jar app.jar

USER app
ENV PORT=8080
EXPOSE 8080

# Container health check (busybox wget is included in the alpine base)
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

# Default: start HTTP mode. Override for CLI use:
#   docker run --rm nihit3003/calculator-devops:latest add 2 3   (see README)
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["serve"]

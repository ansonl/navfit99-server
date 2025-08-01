FROM amazoncorretto:8-alpine-jdk AS builder

# Install Maven
RUN apk add --no-cache maven
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean install

FROM amazoncorretto:8-alpine-jre AS runner

# Copy build artifacts to runtime image
#COPY --from=builder --chown=1000:1000 target /app/
WORKDIR /app
COPY --from=builder /app/target/navfit99-server-1.0-jar-with-dependencies.jar navfit99-server-1.0-jar-with-dependencies.jar
COPY reference/*.accdb ./reference/

# Switch to non-root user
USER 1000:1000
WORKDIR /app

CMD ["java", "-jar", "/app/navfit99-server-1.0-jar-with-dependencies.jar"]
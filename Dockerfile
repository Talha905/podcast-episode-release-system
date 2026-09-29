# ==============================================================================
# Stage 1: Build Application WAR using Maven and Eclipse Temurin JDK 17
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package WAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Optimized Production Runtime Image with Apache Tomcat 10.1 (Jakarta EE 10)
# ==============================================================================
FROM tomcat:10.1-jdk17-temurin-jammy
LABEL maintainer="Talha Patrawala <producer@podcastrelease.com>" \
      version="1.0.0" \
      description="Jenkins-Based Podcast Episode Release System - Production Docker Container"

# Create non-root system user with home directory for security compliance
RUN groupadd -r appgroup && useradd -r -m -d /home/appuser -g appgroup appuser

# Remove default Tomcat webapps for cleaner attack surface
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy built WAR as ROOT.war so application serves from root path (/)
COPY --from=builder /app/target/podcast-release.war /usr/local/tomcat/webapps/ROOT.war

# Create necessary persistent upload, log, and data directories
RUN mkdir -p /opt/podcast-release/uploads /opt/podcast-release/logs /opt/podcast-release/data && \
    chown -R appuser:appgroup /usr/local/tomcat /opt/podcast-release /home/appuser

# Set environment variables
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=dev \
    SPRING_DATASOURCE_URL="jdbc:h2:file:/opt/podcast-release/data/podcastdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" \
    JAVA_OPTS="-Djava.awt.headless=true -Xms256m -Xmx512m -XX:+UseG1GC"

# Switch to non-root user
USER appuser
EXPOSE 8080

# Health Check instruction to validate container readiness
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/login || exit 1

# Start Tomcat Server
CMD ["catalina.sh", "run"]

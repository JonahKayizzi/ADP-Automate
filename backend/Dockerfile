# Build with Docker context = repository root (Render default).
# Example: docker build -f backend/Dockerfile .

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -q -DskipTests package \
  && mv target/adp-automate-*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 appuser \
  && mkdir -p /app/data/uploads /var/data/uploads \
  && chown -R appuser:appuser /app /var/data
COPY --from=build /workspace/app.jar /app/app.jar
USER appuser
EXPOSE 8080
ENV JAVA_OPTS=""
ENV UPLOAD_DIR=/var/data/uploads
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

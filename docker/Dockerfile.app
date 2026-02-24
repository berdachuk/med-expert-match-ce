# Docs build stage: MkDocs static site for /docs
FROM python:3.12-alpine AS docs-builder
WORKDIR /build
COPY requirements-docs.txt mkdocs.yml ./
COPY docs ./docs
RUN pip install --no-cache-dir -r requirements-docs.txt && mkdocs build

# Build stage: Liberica JDK 21 (Debian) + Maven
FROM bellsoft/liberica-openjdk-debian:21 AS builder
WORKDIR /build

RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy dependency definitions first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and embed built docs at static/docs for serving at /docs
COPY src ./src
COPY --from=docs-builder /build/site ./src/main/resources/static/docs

RUN mvn package -B -DskipTests -q && \
    mv target/med-expert-match.jar target/app.jar

# Runtime stage: same Liberica image (compact, TCK-verified)
FROM bellsoft/liberica-openjdk-debian:21
WORKDIR /app

RUN groupadd -r appuser -g 1000 && useradd -r -u 1000 -g appuser appuser

COPY --from=builder /build/target/app.jar ./app.jar
# Built-in skills are now included in the JAR via src/main/resources/skills
# Optional extra skills can still be mounted via MEDEXPERTMATCH_SKILLS_EXTRA_DIRECTORY + volume mount

USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

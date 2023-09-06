FROM eclipse-temurin:11-jre-focal

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*  # Clean up to reduce image size

WORKDIR /app

COPY build/install/blocc-temp-humidity-app/ .
COPY scripts/raft-leader-check.sh .

ENTRYPOINT ["./raft-leader-check.sh "]
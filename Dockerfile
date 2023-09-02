FROM eclipse-temurin:11-jre-focal

WORKDIR /app

COPY build/install/blocc-temp-humidity-app/ .

ENTRYPOINT ["./bin/blocc-temp-humidity-app"]
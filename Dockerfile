FROM gradle:8.3.0-jdk11-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle installDist

# Use an official openjdk image for runtime
FROM eclipse-temurin:11
WORKDIR /app
COPY --from=build /app/build/install/blocc-temp-humidity-app/ .

# Command to run the application
ENTRYPOINT ["./bin/blocc-temp-humidity-app"]
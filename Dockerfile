FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system oneshop && useradd --system --gid oneshop --home /app oneshop
WORKDIR /app
COPY --from=build --chown=oneshop:oneshop /build/target/oneshop-0.0.1-SNAPSHOT.jar /app/oneshop.jar
COPY --chown=oneshop:oneshop deploy/start.sh /app/start.sh
RUN chmod 0555 /app/start.sh
USER oneshop
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -Xss512k"
EXPOSE 8080
ENTRYPOINT ["/app/start.sh"]

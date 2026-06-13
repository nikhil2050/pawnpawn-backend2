FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY pnpn-shared/pom.xml pnpn-shared/
COPY pnpn-gateway/pom.xml pnpn-gateway/
COPY pnpn-user-service/pom.xml pnpn-user-service/
COPY pnpn-catalog-service/pom.xml pnpn-catalog-service/
COPY pnpn-transaction-service/pom.xml pnpn-transaction-service/
COPY pnpn-messaging-service/pom.xml pnpn-messaging-service/
RUN mvn dependency:go-offline -B -pl pnpn-catalog-service -am

COPY . .
RUN mvn package -pl pnpn-catalog-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/pnpn-catalog-service/target/*.jar app.jar

EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]

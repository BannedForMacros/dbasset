# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copiar solo el pom para descargar dependencias y aprovechar el cache de Docker
COPY pom.xml .
RUN mvn dependency:go-offline
# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copiar el jar generado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar
# Exponer el puerto que usa Spring Boot
EXPOSE 8080
# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
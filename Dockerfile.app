#### CREACION DEL JAR #### PARA COMPILAR
FROM maven:3.10.0-eclipse-temurin-25-alpine AS builder

WORKDIR /app
COPY ./pom.xml .
#COPY ./lombok.config ./
RUN mvn -e -B dependency:go-offline
COPY ./src ./src
RUN mvn -e -B -Dmaven.test.skip=true package

####FASE FINAL DE LA IMAGEN ####SOLO LO NECESARIO PARA EJECUTAR
FROM eclipse-temurin:25-jre-alpine

WORKDIR /workspace

ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=spring_ai_rag
ENV DB_USER=postgres
ENV OLLAMA_URL=http://host.docker.internal:11434
ENV CHAT_MODEL=qwen3:0.6b
ENV EMBEDDING_MODEL=nomic-embed-text

COPY --from=builder /app/target/springai-rag*.jar app.jar

#no abre ningun puerto, es solo informativo
EXPOSE 8080 
ENTRYPOINT exec java -jar /workspace/app.jar
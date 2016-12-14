FROM java:openjdk-8-alpine

COPY ./target/cqlTranslationServer-1.0-SNAPSHOT-jar-with-dependencies.jar /app/
EXPOSE 8080
CMD java -jar /app/cqlTranslationServer-1.0-SNAPSHOT-jar-with-dependencies.jar

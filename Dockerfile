FROM java:openjdk-8-alpine

COPY ./target/cqlTranslationServer-1.0-SNAPSHOT-jar-with-dependencies.jar /app/
CMD java -jar /app/cqlTranslationServer-1.0-SNAPSHOT-jar-with-dependencies.jar

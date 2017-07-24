FROM nexus.informatik.haw-hamburg.de/java:8-jre
ADD target/gis-data-svc-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]

FROM artifactory.mars.haw-hamburg.de:5000/java:8-jre
ADD target/gis-data-svc-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]

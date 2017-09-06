FROM nexus.informatik.haw-hamburg.de/openjdk:8-jre-alpine
ADD target/gis-data-svc-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=production","-jar","/app/app.jar"]

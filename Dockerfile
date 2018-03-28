FROM nexus.informatik.haw-hamburg.de/openjdk:8-jre-alpine

RUN apk add --no-cache gdal --repository http://nl.alpinelinux.org/alpine/edge/testing

ADD target/gis-data-svc-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=production","-jar","/app/app.jar"]

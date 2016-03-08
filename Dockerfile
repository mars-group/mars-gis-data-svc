FROM artifactory.mars.haw-hamburg.de:5000/maven:3.3.3-jdk-8

COPY [".", "/gisimport"]
WORKDIR /gisimport
RUN mvn -Dmaven.test.skip=true install
RUN cp target/gis-import-0.0.1-SNAPSHOT.jar /app.jar
RUN rm -rf /gisimport

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

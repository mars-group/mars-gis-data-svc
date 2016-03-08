FROM artifactory.mars.haw-hamburg.de:5000/niaquinto/gradle

COPY [".", "/gisimport"]
WORKDIR /gisimport
RUN mvn package
ADD target/gis-import-0.0.1-SNAPSHOT.jar app.jar
RUN rm -rf /gisimport

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

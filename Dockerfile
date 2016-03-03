FROM artifactory.mars.haw-hamburg.de:5000/niaquinto/gradle

COPY [".", "/gisimport"]
WORKDIR /gisimport
RUN gradle build
RUN cp build/libs/mars-gis-import-0.0.1-SNAPSHOT.jar /app.jar
RUN rm -rf /gisimport

EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

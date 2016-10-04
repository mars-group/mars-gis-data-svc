package org.mars_group.gisimport.util;


import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.Random;

@Component
public class GeoServerInstance {

    private GeoServerRESTPublisher publisher;
    private GeoServerRESTReader reader;
    private String URI;
    private final String USER = "admin";
    private final String PASSWORD = "geoserver";

    @Autowired
    private EurekaClient eurekaClient;

    private void init() throws MalformedURLException, GisImportException {
        URI = getRandomGeoServerInstanceUri();

        reader = new GeoServerRESTReader(URI, USER, PASSWORD);
        publisher = new GeoServerRESTPublisher(URI, USER, PASSWORD);
    }

    private String getRandomGeoServerInstanceUri() throws GisImportException {

        Application app = eurekaClient.getApplication("geoserver");
        int numberOfInstances = app.getInstances().size();

        if (numberOfInstances < 1) {
            throw new GisImportException("No GeoServer Instance found!");
        }

        Random rnd = new Random();
        int instanceIndex = rnd.nextInt(numberOfInstances);
        String instanceId = app.getInstances().get(instanceIndex).getId();

        String ip = app.getByInstanceId(instanceId).getIPAddr();
        int port = app.getByInstanceId(instanceId).getPort();

        return "http://" + ip + ":" + port + "/geoserver";
    }

    public GeoServerRESTPublisher getPublisher() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            init();
        }
        return publisher;
    }

    public GeoServerRESTReader getReader() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            init();
        }
        return reader;
    }

    public String getUri() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            init();
        }
        return URI;
    }

    public String getUser() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            init();
        }
        return USER;
    }

    public String getPassword() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            init();
        }
        return PASSWORD;
    }
}

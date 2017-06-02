package org.mars_group.gisimport.util;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
public class GeoServer {

    private GeoServerRESTPublisher publisher;
    private GeoServerRESTReader reader;
    private final String baseUrl = "http://geoserver/geoserver";

    private void init() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            String URI = baseUrl;
            String USER = "admin";
            String PASSWORD = "geoserver";
            reader = new GeoServerRESTReader(URI, USER, PASSWORD);
            publisher = new GeoServerRESTPublisher(URI, USER, PASSWORD);
        }
    }

    public GeoServerRESTPublisher getPublisher() throws MalformedURLException, GisImportException {
        init();
        return publisher;
    }

    public GeoServerRESTReader getReader() throws MalformedURLException, GisImportException {
        init();
        return reader;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

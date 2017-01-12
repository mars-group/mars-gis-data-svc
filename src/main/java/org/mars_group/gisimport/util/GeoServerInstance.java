package org.mars_group.gisimport.util;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
public class GeoServerInstance {

    private GeoServerRESTPublisher publisher;
    private GeoServerRESTReader reader;
    private String URI;
    private final String USER = "admin";
    private final String PASSWORD = "geoserver";

    private void init() throws MalformedURLException, GisImportException {
        if (publisher == null) {
            URI = "http://geoserver/geoserver";
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
}

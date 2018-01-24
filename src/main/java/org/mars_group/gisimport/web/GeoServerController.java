package org.mars_group.gisimport.web;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
public class GeoServerController {

    private GeoServerRESTPublisher publisher;
    private GeoServerRESTReader reader;

    private String URI = "http://geoserver/geoserver";
    private String USER = "admin";
    private String PASSWORD = "geoserver";

    public GeoServerRESTPublisher getPublisher() {
        if (publisher == null) {
            publisher = new GeoServerRESTPublisher(URI, USER, PASSWORD);
        }
        return publisher;
    }

    public GeoServerRESTReader getReader() throws MalformedURLException {
        if (reader == null) {
            reader = new GeoServerRESTReader(URI, USER, PASSWORD);
        }
        return reader;
    }
}

package org.mars_group.gisimport.controller;

import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;

@Component
class GeoServerExport {

    private final GeoServerInstance geoServerInstance;

    @Autowired
    GeoServerExport(GeoServerInstance geoServerInstance) {
        this.geoServerInstance = geoServerInstance;
    }

    String getUri(String dataId, String name) throws MalformedURLException, GisImportException {
        GeoServerRESTReader reader = geoServerInstance.getReader();
        RESTLayer layer = reader.getLayer(dataId, name);
        RESTLayer.Type type = layer.getType();

        if (type == RESTLayer.Type.VECTOR) {
            return UriBuilder.fromUri("")
                    .path("wfs")
                    .queryParam("request", "GetFeature")
                    .queryParam("version", "2.0.0")
                    .queryParam("typeName", dataId + ":" + name)
                    .queryParam("outputFormat", "shape-zip")
                    .build().toString();

        } else {
            return UriBuilder.fromUri("")
                    .path("wcs")
                    .queryParam("request", "GetCoverage")
                    .queryParam("service", "WCS")
                    .queryParam("version", "2.0.1")
                    .queryParam("coverageId", dataId + ":" + name)
                    .queryParam("format", "ArcGrid")
                    .build().toString();
        }
    }
}

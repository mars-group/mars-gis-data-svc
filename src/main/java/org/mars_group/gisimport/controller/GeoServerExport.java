package org.mars_group.gisimport.controller;

import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import org.mars_group.core.Metadata;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.mars_group.metadataclient.MetadataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;

@Component
class GeoServerExport {

    private final GeoServerInstance geoServerInstance;
    private final RestTemplate restTemplate;

    @Autowired
    public GeoServerExport(RestTemplate restTemplate, GeoServerInstance geoServerInstance) {
        this.restTemplate = restTemplate;
        this.geoServerInstance = geoServerInstance;
    }

    String getUri(String dataId) throws MalformedURLException, GisImportException {
        MetadataClient metadataClient = MetadataClient.getInstance(restTemplate);
        Metadata metadata = metadataClient.getMetadata(dataId);
        String title = metadata.getTitle();

        GeoServerRESTReader reader = geoServerInstance.getReader();
        RESTLayer layer = reader.getLayer(dataId, title);
        RESTLayer.Type type = layer.getType();

        if (type == RESTLayer.Type.VECTOR) {
            return UriBuilder.fromUri("")
                    .path("wfs")
                    .queryParam("request", "GetFeature")
                    .queryParam("version", "2.0.0")
                    .queryParam("typeName", dataId + ":" + title)
                    .queryParam("outputFormat", "shape-zip")
                    .build().toString();

        } else {
            return UriBuilder.fromUri("")
                    .path("wcs")
                    .queryParam("request", "GetCoverage")
                    .queryParam("service", "WCS")
                    .queryParam("version", "2.0.1")
                    .queryParam("coverageId", dataId + ":" + title)
                    .queryParam("format", "ArcGrid")
                    .build().toString();
        }
    }
}

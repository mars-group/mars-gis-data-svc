package org.mars_group.gisimport.controller;

import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import org.apache.commons.io.FilenameUtils;
import org.mars_group.core.Metadata;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServer;
import org.mars_group.metadataclient.MetadataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

@Component
class GeoServerExport {

    private final GeoServer geoServer;
    private final MetadataClient metadataClient;

    @Autowired
    public GeoServerExport(RestTemplate restTemplate, GeoServer geoServer) {
        this.geoServer = geoServer;
        this.metadataClient = new MetadataClient(restTemplate);
    }

    URI getUriFromDataId(String dataId) {
        Metadata metadata = metadataClient.getMetadata(dataId);
        Map typeSpecificFields = metadata.getAdditionalTypeSpecificData();

        return URI.create(typeSpecificFields.get("uri").toString());
    }

    URI generateRasterUri(String dataId, String title) throws MalformedURLException, GisImportException {
        GeoServerRESTReader reader = geoServer.getReader();
        String baseName = FilenameUtils.getBaseName(title);

        RESTLayer layer = reader.getLayer(dataId, baseName);

        if (layer == null) {
            System.out.println("Layer is null");
            return URI.create("");
        }

        return UriBuilder.fromUri("")
                .path("wcs")
                .queryParam("request", "GetCoverage")
                .queryParam("service", "WCS")
                .queryParam("version", "2.0.1")
                .queryParam("coverageId", dataId + ":" + baseName)
                .queryParam("format", "ArcGrid-GZIP")
                .build();
    }

    URI generateVectorUri(String dataId, String dataName) {
        return UriBuilder.fromUri("")
                .path("wfs")
                .queryParam("request", "GetFeature")
                .queryParam("version", "2.0.0")
                .queryParam("typeName", dataId + ":" + dataName)
                .queryParam("outputFormat", "application/json")
                .build();
    }
}

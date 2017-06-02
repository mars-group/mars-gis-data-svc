package org.mars_group.gisimport.controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GeoServer;
import org.mars_group.gisimport.util.GisManager;
import org.mars_group.gisimport.util.GisType;
import org.mars_group.metadataclient.MetadataClient;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

@Component
class GeoServerImport {

    private final RestTemplate restTemplate;
    private final GeoServer geoServer;
    private final GeoServerExport geoServerExport;

    @Autowired
    public GeoServerImport(RestTemplate restTemplate, GeoServer geoServer, GeoServerExport geoServerExport) {
        this.restTemplate = restTemplate;
        this.geoServer = geoServer;
        this.geoServerExport = geoServerExport;
    }

    void handleImport(String uploadDir, String uploadFilename, String dataId, String title) throws GisImportException, MalformedURLException {
        title = title.replaceAll(" ", "");

        File file = new File(uploadDir + File.separator + uploadFilename);
        GisManager gisManager;

        try {
            gisManager = new GisManager(uploadDir, file.getAbsolutePath());
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }

        String crsCode;
        try {
            CoordinateReferenceSystem crs = gisManager.getCoordinateReferenceSystem();
            crsCode = CRS.lookupIdentifier(crs, true);
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }

        boolean importSuccess = false;
        GeoServerRESTPublisher publisher = geoServer.getPublisher();
        publisher.createWorkspace(dataId);

        GisType gisType = gisManager.getGisType();

        MetadataClient metadataClient = new MetadataClient(restTemplate);

        Map<String, Double> topLeftBound = new HashMap<>();
        topLeftBound.put("lat", gisManager.getTopRightBound()[0]);
        topLeftBound.put("lng", gisManager.getTopRightBound()[1]);

        Map<String, Double> bottomRightBound = new HashMap<>();
        bottomRightBound.put("lat", gisManager.getBottomLeftBound()[0]);
        bottomRightBound.put("lng", gisManager.getBottomLeftBound()[1]);

        Map<String, Object> additionalTypeSpecificData = new HashMap<>();
        additionalTypeSpecificData.put("topLeftBound", topLeftBound);
        additionalTypeSpecificData.put("bottomRightBound", bottomRightBound);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", gisType.getName());

        String dataName = gisManager.getDataName();

        try {
            String storeName = "WebUI_" + gisType.getName();
            String defaultStyle = "default_point";
            switch (gisType) {
                case ASC:
                    // We converted the Ascii Grid to GeoTiff, so this imports Geotiff
                    // intended fallthrough
                case TIF:
                    file = new File(uploadDir + File.separator + dataName + ".tif");
                    importSuccess = publisher.publishGeoTIFF(dataId, storeName, title, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, defaultStyle, null);
                    additionalTypeSpecificData.put("uri", geoServerExport.createRasterUri(dataId, title).toString());
                    break;
                case SHP:
                    importSuccess = publisher.publishShp(dataId, storeName, dataName, file, crsCode, defaultStyle);
                    additionalTypeSpecificData.put("uri", geoServerExport.createVectorUri(dataId, dataName).toString());
                    break;
            }

            metadata.put("additionalTypeSpecificData", additionalTypeSpecificData);
            metadataClient.updateMetadata(dataId, metadata);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            publisher.removeWorkspace(dataId, false);
            throw new GisImportException(e.getMessage());
        }

        if (!importSuccess) {
            publisher.removeWorkspace(dataId, false);
            throw new GisImportException(uploadFilename + ": error inside the GeoServer! Import failed");
        }
    }

}

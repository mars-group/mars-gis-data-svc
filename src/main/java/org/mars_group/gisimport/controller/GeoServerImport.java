package org.mars_group.gisimport.controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.apache.commons.io.FilenameUtils;
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

    void handleImport(String uploadDir, String uploadFilename, String dataId, String title) throws GisImportException,
            MalformedURLException {
        File file = new File(uploadDir + File.separator + uploadFilename);

        GisManager gisManager = getGisManager(uploadDir, file);
        GisType gisType = gisManager.getGisType();
        String dataName = gisManager.getDataName();

        title = title.replaceAll(" ", "");

        boolean importSuccess = false;

        GeoServerRESTPublisher publisher = geoServer.getPublisher();
        publisher.createWorkspace(dataId);

        String crsCode = getCrsCode(gisManager);

        Map<String, Object> additionalTypeSpecificData = calculateMetadataBounds(gisManager);

        try {
            switch (gisType) {
                case ASC:
                    // We converted the Ascii Grid to GeoTiff, so this imports Geotiff
                case TIF:
                    String baseName = FilenameUtils.getBaseName(title);

                    file = new File(uploadDir + File.separator + dataName + ".tif");

                    importSuccess = publisher.publishGeoTIFF(dataId, "Webui_Raster", baseName, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);

                    additionalTypeSpecificData.put("uri", geoServerExport.generateRasterUri(dataId, title).toString());
                    break;
                case GJSON:
                    // Todo: Implement
                    throw new GisImportException("GeoJSON is not supported by the GeoServer, so this does not work.");
                case SHP:
                    importSuccess = publisher.publishShp(dataId, "Webui_Vector", dataName, file, crsCode,
                            "default_point");

                    additionalTypeSpecificData.put("uri", geoServerExport.generateVectorUri(dataId,
                            dataName).toString());
                    break;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            handleFailedImport(dataId, publisher, uploadFilename + ": " + e.getMessage());
            return;
        }

        if (!importSuccess) {
            handleFailedImport(dataId, publisher, uploadFilename + ": error inside the GeoServer! Import failed");
        }

        writeMetadata(dataId, gisType, additionalTypeSpecificData);
    }

    private GisManager getGisManager(String uploadDir, File file) throws GisImportException {
        GisManager gisManager;
        try {
            gisManager = new GisManager(uploadDir, file.getAbsolutePath());
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }
        return gisManager;
    }

    private String getCrsCode(GisManager gisManager) throws GisImportException {
        String crsCode;
        try {
            CoordinateReferenceSystem crs = gisManager.getCoordinateReferenceSystem();
            crsCode = CRS.lookupIdentifier(crs, true);
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }
        return crsCode;
    }

    private void writeMetadata(String dataId, GisType gisType, Map<String, Object> additionalTypeSpecificData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", gisType.getName());
        metadata.put("additionalTypeSpecificData", additionalTypeSpecificData);

        MetadataClient metadataClient = new MetadataClient(restTemplate);
        metadataClient.updateMetadata(dataId, metadata);
    }

    private void handleFailedImport(String dataId, GeoServerRESTPublisher publisher, String message)
            throws GisImportException {
        publisher.removeWorkspace(dataId, false);
        throw new GisImportException(message);
    }

    private Map<String, Object> calculateMetadataBounds(GisManager gisManager) {
        Map<String, Double> topLeftBound = new HashMap<>();
        topLeftBound.put("lat", gisManager.getTopRightBound()[0]);
        topLeftBound.put("lng", gisManager.getTopRightBound()[1]);

        Map<String, Double> bottomRightBound = new HashMap<>();
        bottomRightBound.put("lat", gisManager.getBottomLeftBound()[0]);
        bottomRightBound.put("lng", gisManager.getBottomLeftBound()[1]);

        Map<String, Object> additionalTypeSpecificData = new HashMap<>();
        additionalTypeSpecificData.put("topLeftBound", topLeftBound);
        additionalTypeSpecificData.put("bottomRightBound", bottomRightBound);
        return additionalTypeSpecificData;
    }

}

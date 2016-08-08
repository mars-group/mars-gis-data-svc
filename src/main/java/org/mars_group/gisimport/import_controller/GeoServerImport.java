package org.mars_group.gisimport.import_controller;


import de.haw_hamburg.mars.mars_group.metadataclient.MetadataClient;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.export_controller.FileDownloadController;
import org.mars_group.gisimport.util.DataType;
import org.mars_group.gisimport.util.GeoServerInstance;
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

    private final
    RestTemplate restTemplate;

    private final
    GeoServerInstance geoServerInstance;

    private final
    FileDownloadController downloadController;

    @Autowired
    public GeoServerImport(RestTemplate restTemplate, GeoServerInstance geoServerInstance, FileDownloadController downloadController) {
        this.restTemplate = restTemplate;
        this.geoServerInstance = geoServerInstance;
        this.downloadController = downloadController;
    }

    void handleImport(String uploadDir, String uploadFilename, String dataId, String title) throws GisImportException, MalformedURLException {
        title = title.replaceAll(" ", "");

        File file = new File(uploadDir + File.separator + uploadFilename);
        GisValidator gisValidator;

        try {
            gisValidator = new GisValidator(uploadDir, file.getAbsolutePath());
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }

        CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
        String crsCode;

        try {
            crsCode = CRS.lookupIdentifier(crs, true);
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }

        boolean result = false;
        GeoServerRESTPublisher publisher = geoServerInstance.getPublisher();
        publisher.createWorkspace(dataId);

        DataType dataType = gisValidator.getDataType();

        MetadataClient metadataClient = MetadataClient.getInstance(restTemplate, "http://metadata-service:4444");

        Map<String, Double> topLeftBound = new HashMap<>();
        topLeftBound.put("lat", gisValidator.getTopRightBound()[0]);
        topLeftBound.put("lng", gisValidator.getTopRightBound()[1]);

        Map<String, Double> bottomRightBound = new HashMap<>();
        bottomRightBound.put("lat", gisValidator.getBottomLeftBound()[0]);
        bottomRightBound.put("lng", gisValidator.getBottomLeftBound()[1]);

        Map<String, Object> additionalTypeSpecificData = new HashMap<>();
        additionalTypeSpecificData.put("topLeftBound", topLeftBound);
        additionalTypeSpecificData.put("bottomRightBound", bottomRightBound);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", dataType.getName());
        metadata.put("additionalTypeSpecificData", additionalTypeSpecificData);

        try {
            switch (dataType) {
                case ASC:
                    // We converted the Ascii Grid to GeoTiff, so this imports Geotiff
                    file = new File(uploadDir + File.separator + gisValidator.getDataName() + ".tif");
                    result = publisher.publishGeoTIFF(dataId, "Websuite_Asc", title, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    metadata.put("uri", downloadController.downloadRasterFile(dataId, title));
                    break;
                case SHP:
                    result = publisher.publishShp(dataId, "Websuite_Shapefile", gisValidator.getDataName(),
                            file, crsCode, "default_point");
                    metadata.put("uri", downloadController.downloadVectorFile(dataId, gisValidator.getDataName()));
                    break;
                case TIF:
                    file = new File(uploadDir + File.separator + gisValidator.getDataName() + ".tif");
                    result = publisher.publishGeoTIFF(dataId, "Websuite_GeoTiff", title, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    metadata.put("uri", downloadController.downloadRasterFile(dataId, title));
                    break;
            }
            metadataClient.updateMetaData(dataId, metadata);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            publisher.removeWorkspace(dataId, false);
            throw new GisImportException(e.getMessage());
        }

        if (!result) {
            publisher.removeWorkspace(dataId, false);
            throw new GisImportException(uploadFilename + ": error inside the GeoServer! Import failed");
        }
    }

}

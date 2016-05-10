package org.mars_group.gisimport.import_controller;


import de.haw_hamburg.mars.mars_group.metadataclient.MetadataClient;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.apache.commons.io.FilenameUtils;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.export_controller.FileDownloadController;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.mars_group.gisimport.util.UploadType;
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

    @Autowired
    GeoServerInstance geoServerInstance;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    FileDownloadController downloadController;

    void handleImport(String uploadDir, String uploadFilename, String importId, String layername) throws GisImportException, MalformedURLException {

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
        publisher.createWorkspace(importId);

        UploadType uploadType = gisValidator.getUploadType();

        MetadataClient metadataClient = MetadataClient.getInstance(restTemplate, "http://metadata:4444");

        Map<String, Object> map = new HashMap<>();
        map.put("type", uploadType.getName());

        try {
            switch (uploadType) {
                case ASC:
                    // We converted the Ascii Grid to GeoTiff, so this imports Geotiff
                    file = new File(uploadDir + File.separator + FilenameUtils.getBaseName(uploadFilename) + ".tif");
                    result = publisher.publishGeoTIFF(importId, "Websuite_Asc", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    map.put("uri", downloadController.downloadRasterFile(importId, layername));
                    break;
                case SHP:
                    result = publisher.publishShp(importId, "Websuite_Shapefile", gisValidator.getDatasetName(),
                            file, crsCode, "default_point");
                    map.put("uri", downloadController.downloadVectorFile(importId, gisValidator.getDatasetName()));
                    break;
                case TIF:
                    file = new File(uploadDir + File.separator + FilenameUtils.getBaseName(uploadFilename) + ".tif");
                    result = publisher.publishGeoTIFF(importId, "Websuite_GeoTiff", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    map.put("uri", downloadController.downloadRasterFile(importId, layername));
                    break;
            }
            metadataClient.updateMetaData(importId, map);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            publisher.removeWorkspace(importId, false);
            throw new GisImportException(e.getMessage());
        }

        if (!result) {
            publisher.removeWorkspace(importId, false);
            throw new GisImportException(uploadFilename + ": error inside the GeoServer! Import failed");
        }
    }

}

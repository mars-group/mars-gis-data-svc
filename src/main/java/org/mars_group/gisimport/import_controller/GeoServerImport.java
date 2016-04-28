package org.mars_group.gisimport.import_controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.apache.commons.io.FilenameUtils;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.mars_group.gisimport.util.UploadType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

@Component
class GeoServerImport {

    @Autowired
    GeoServerInstance geoServerInstance;

    void handleImport(String uploadDir, String uploadFilename, UploadType uploadType, String importId, String layername) throws GisImportException, MalformedURLException {

        File file = new File(uploadDir + File.separator + uploadFilename);
        GisValidator gisValidator;

        try {
            gisValidator = new GisValidator(uploadDir, file.getAbsolutePath(), uploadType);
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

        if (uploadType == null) {
            uploadType = gisValidator.getUploadType();
        }

        try {
            switch (uploadType) {
                case ASC:
                    // We converted the Ascii Grid to GeoTiff, so this imports Geotiff
                    file = new File(uploadDir + File.separator + FilenameUtils.getBaseName(uploadFilename) + ".tif");
                    result = publisher.publishGeoTIFF(importId, "Websuite_Asc", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    break;
                case GEOTIFF:
                    file = new File(uploadDir + File.separator + FilenameUtils.getBaseName(uploadFilename) + ".tif");
                    result = publisher.publishGeoTIFF(importId, "Websuite_GeoTiff", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    break;
                case SHP:
                    result = publisher.publishShp(importId, "Websuite_Shapefile", gisValidator.getDatasetName(),
                            file, crsCode, "default_point");
                    break;
            }
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

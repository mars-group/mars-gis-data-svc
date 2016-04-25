package org.mars_group.gisimport.import_controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
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

    String handleImport(String uploadDir, String zipFilename, UploadType uploadType, String importId, String layername) throws GisImportException, MalformedURLException {

        File file = new File(uploadDir + "/" + zipFilename);
        GisValidator gisValidator;

        try {
            gisValidator = new GisValidator(uploadDir, file.getAbsolutePath(), uploadType);
            gisValidator.validate();
        } catch (IOException | GisValidationException e) {
            file.delete();
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }

        CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
        String crsCode;

        if(crs == null) {
            crsCode = "EPSG:4326";
        } else {
            try {
                crsCode = CRS.lookupIdentifier(crs, true);
            } catch (FactoryException e) {
                e.printStackTrace();
                throw new GisImportException(e.getMessage());
            }
        }

        boolean result = false;
        GeoServerRESTPublisher publisher = geoServerInstance.getPublisher();
        publisher.createWorkspace(importId);

        try {
            switch (uploadType) {
                case SHP:
                    result = publisher.publishShp(importId, "Websuite_Shapefile", gisValidator.getDatasetName(),
                            file, crsCode, "default_point");
                    break;
                case ASC:
                    result = publisher.publishArcGrid(importId, "Websuite_Asc", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    break;
                case GEOTIFF:
                    result = publisher.publishGeoTIFF(importId, "Websuite_GeoTiff", layername, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);
                    break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            file.delete();
            publisher.removeWorkspace(importId, false);
            throw new GisImportException(e.getMessage());
        }

        if (result) {
            return zipFilename + " Import successfull!";
        } else {
            file.delete();
            publisher.removeWorkspace(importId, false);
            throw new GisImportException(zipFilename + ": error inside the GeoServer! Import failed");
        }
    }

}

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

    String handleImport(String uploadDir, String zipFilename, UploadType uploadType) throws GisImportException, MalformedURLException {

        File file = new File(uploadDir + "/" + zipFilename);
        GisValidator gisValidator;

        try {
            gisValidator = new GisValidator(uploadDir, file.getAbsolutePath());
            gisValidator.validate();
        } catch (IOException e) {
            file.delete();
            e.printStackTrace();
            return e.getMessage();
        } catch (GisValidationException e) {
            file.delete();
            e.printStackTrace();
            return e.getMessage();
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
                return e.getMessage();
            }
        }

        GeoServerRESTPublisher publisher = geoServerInstance.getPublisher();
        String datasetName = gisValidator.getDatasetName();
        boolean result = false;

        try {
            switch (uploadType) {
                case SHP:
                    result = publisher.publishShp("myWorkspace", "myStore", datasetName, file, crsCode, "default_point");
                    break;
                case ASC:
                    result = publisher.publishArcGrid("myWorkspace", datasetName, datasetName, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED, "default_point", null);
                    break;
                case GEOTIFF:
                    result = publisher.publishGeoTIFF("myWorkspace", datasetName, datasetName, file, crsCode,
                            GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED, "default_point", null);
                    break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            file.delete();
            return "File not found!";
        }

        if (result) {
            return zipFilename + " Import successfull!";
        } else {
            return zipFilename + " error inside the GeoServer! Import failed";
        }
    }

}

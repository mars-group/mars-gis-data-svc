package org.mars_group.gisimport.import_controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UploadType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

class GeoServerImport {

    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;

   GeoServerImport() {
        final String GEOSERVER_URL = "http://geoserver:8080/geoserver";
        final String RESTUSER = "admin";
        final String RESTPW = "geoserver";

        try {
            reader = new GeoServerRESTReader(GEOSERVER_URL, RESTUSER, RESTPW);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        publisher = new GeoServerRESTPublisher(GEOSERVER_URL, RESTUSER, RESTPW);

        if (!reader.getWorkspaceNames().contains("myWorkspace")) {
            publisher.createWorkspace("myWorkspace");
        }
    }

    String handleImport(String zipFilename, UploadType uploadType) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);

        GisValidator gisValidator;
        try {
            gisValidator = new GisValidator(file.getAbsolutePath());
            gisValidator.validate();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (GisValidationException e) {
            file.delete();
            e.printStackTrace();
            return e.getMessage();
        }

        CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();

        String datasetName = gisValidator.getDatasetName();

        boolean result = false;
        try {
            switch (uploadType) {
                case SHP:
//                    result = publisher.publishShp("myWorkspace", "myStore", datasetName, file, crs.toWKT(), "giant_polygon");
                    result = publisher.publishShp("myWorkspace", datasetName, datasetName, file);
                    break;
                case ASC:
                    result = false;
                    break;
                case GEOTIFF:
                    result = publisher.publishGeoTIFF("myWorkspace", datasetName, datasetName, file, crs.toWKT(),
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

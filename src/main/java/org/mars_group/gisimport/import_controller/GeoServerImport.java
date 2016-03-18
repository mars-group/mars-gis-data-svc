package org.mars_group.gisimport.import_controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

class GeoServerImport {

    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;

    GeoServerImport() {

        String GEOSERVER_URL;
        if (System.getenv("GEOSERVER") != null && System.getenv("GEOSERVER").equals("local")) {
            GEOSERVER_URL = "http://192.168.99.100:8080/geoserver";
        } else {
            GEOSERVER_URL = "http://dock-two.mars.haw-hamburg.de:8080/geoserver";
        }
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

    String importShp(String zipFilename) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);

        GisValidator gisValidator = new GisValidator(file);

        if (gisValidator.zipHasDirectory()) {
            file.delete();
            return "Directory not allowed inside Zip file!";
        }

        String srs = gisValidator.getCoordinateReferenceSystem();
        String datasetName = gisValidator.getDatasetName();

        boolean result;
        try {
            result = publisher.publishShp("myWorkspace", "myStore", datasetName, file, srs, "giant_polygon");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            file.delete();
            return "File not found!";
        }

        return handleCallback(file, zipFilename, result);
    }

    String importGeoTiff(String zipFilename, String datasetName) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);

        boolean result;
        try {
            result = publisher.publishGeoTIFF("myWorkspace", datasetName, datasetName, file, "EPSG:4326",
                    GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED, "default_point", null);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            file.delete();
            return "File not found!";
        }

        return handleCallback(file, zipFilename, result);
    }

    private String handleCallback(File file, String zipFilename, boolean result) {
        file.delete();

        if (result) {
            return zipFilename + " was imported successfull!";
        } else {
            return "error inside the GeoServer!";
        }
    }

}

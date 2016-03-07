package org.mars_group.upload;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class GeoServerImport {

    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;

    public GeoServerImport() {

        final String RESTURL = "http://192.168.99.100:8080/geoserver";
//        final String RESTURL = "http://dock-one.mars.haw-hamburg.de:8080/geoserver";
        final String RESTUSER = "admin";
        final String RESTPW = "geoserver";

        try {
            reader = new GeoServerRESTReader(RESTURL, RESTUSER, RESTPW);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        publisher = new GeoServerRESTPublisher(RESTURL, RESTUSER, RESTPW);

        if (!reader.getWorkspaceNames().contains("myWorkspace")) {
            publisher.createWorkspace("myWorkspace");
        }
    }

    public String importShp(String zipFilename, String datasetName) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);
        String error = "";
        boolean result = false;

        try {
            result = publisher.publishShp("myWorkspace", "myStore", datasetName, file, "EPSG:4326", "giant_polygon");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            error = "File not found!";
        } finally {
            file.delete();
        }

        return handleCallback(zipFilename, error, result);
    }

    public String importGeoTiff(String zipFilename, String datasetName) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);
        String error = "";
        boolean result = false;

        try {
            result = publisher.publishGeoTIFF("myWorkspace", "myStore", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            error = "File not found!";
        } finally {
            file.delete();
        }

        return handleCallback(zipFilename, error, result);
    }

    private String handleCallback(String zipFilename, String error, boolean result) {
        if (error.length() > 0) {
            return error;
        } else if (!result) {
            return "error inside the GeoServer!";
        } else {
            return zipFilename + " was imported successfull!";
        }
    }

}

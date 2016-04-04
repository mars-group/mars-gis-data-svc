package org.mars_group.gisimport.import_controller;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UploadType;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class GeoServerImport {

    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;

   public GeoServerImport() {
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

    public String handleImport(String zipFilename, UploadType uploadType) {
        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);

        GisValidator gisValidator;
        try {
            gisValidator = new GisValidator(file.getAbsolutePath());
        } catch (GisValidationException e) {
            file.delete();
            e.printStackTrace();
            return e.getMessage();
        }

        String srs = gisValidator.getSpatialReferenceSystem();

        String resultMessage = "";
        // fallback to WGS_84
        if(srs == null) {
            resultMessage = "FALLBACK TO WGS 84";
            srs = "EPSG:4326";
        }

        String datasetName = gisValidator.getDatasetName();

        boolean result = false;
        try {
            switch (uploadType) {
                case SHP:
//                    result = publisher.publishShp("myWorkspace", "myStore", datasetName, file, srs, "giant_polygon");
                    result = publisher.publishShp("myWorkspace", datasetName, datasetName, file);
                    break;
                case ASC:
                    result = false;
                    break;
                case GEOTIFF:
                    result = publisher.publishGeoTIFF("myWorkspace", datasetName, datasetName, file, srs,
                            GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED, "default_point", null);
                    break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            file.delete();
            return "File not found!";
        }

        if (result) {
            return zipFilename + " " + resultMessage + " Import successfull!";
        } else {
            return zipFilename + " " + resultMessage + " error inside the GeoServer! Import failed";
        }
    }


}

package org.mars_group.gisimport.import_controller;


import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UploadType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

@Component
class GeoServerImport {

    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;

    @Autowired
    private EurekaClient discoveryClient;

    String handleImport(String zipFilename, UploadType uploadType) throws GisImportException, MalformedURLException {
        initGeoserver();

        File file = new File(FileUploadController.uploadDir + "/" + zipFilename);

        GisValidator gisValidator;
        try {
            gisValidator = new GisValidator(file.getAbsolutePath());
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

    private void initGeoserver() throws GisImportException, MalformedURLException {
        final String GEOSERVER_URI = getRandomGeoServerInstanceUri();
        final String RESTUSER = "admin";
        final String RESTPW = "geoserver";

        reader = new GeoServerRESTReader(GEOSERVER_URI, RESTUSER, RESTPW);
        publisher = new GeoServerRESTPublisher(GEOSERVER_URI, RESTUSER, RESTPW);

        if (!reader.getWorkspaceNames().contains("myWorkspace")) {
            publisher.createWorkspace("myWorkspace");
        }
    }

    private String getRandomGeoServerInstanceUri() throws GisImportException {
        Application app = discoveryClient.getApplication("geoserver");
        int numberOfInstances = app.getInstances().size();

        if(numberOfInstances < 1) {
            throw new GisImportException("No GeoServer Instance found!");
        }

        Random rnd = new Random();
        int instanceIndex = rnd.nextInt(numberOfInstances);
        String instanceId = app.getInstances().get(instanceIndex).getId();

        String ip = app.getByInstanceId(instanceId).getIPAddr();
        int port = app.getByInstanceId(instanceId).getPort();

        return "http://" + ip + ":" + port + "/geoserver";
    }
}

package org.mars_group.gisimport.service;


import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.referencing.CRS;
import org.mars_group.core.ImportState;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GisType;
import org.mars_group.gisimport.util.GisValidator;
import org.mars_group.gisimport.web.GeoServerController;
import org.mars_group.metadataclient.MetadataClient;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Component
public class GeoServerImport {

    private final RestTemplate restTemplate;
    private final GeoServerController geoServerController;
    private final GeoServerExport geoServerExport;
    private GeoServerRESTPublisher publisher;
    private MetadataClient metadataClient;
    private String dataId;
    private String uploadDir = "upload-dir";
    private String specificUploadDir;
    private String filename;

    @Autowired
    public GeoServerImport(RestTemplate restTemplate, GeoServerController geoServerController,
                           GeoServerExport geoServerExport) {
        this.restTemplate = restTemplate;
        this.geoServerController = geoServerController;
        this.geoServerExport = geoServerExport;
        this.metadataClient = new MetadataClient(restTemplate);
    }

    public void downloadFile(String dataId, String filename) throws IOException {
        specificUploadDir = uploadDir + File.separator + dataId;

        this.dataId = dataId;
        this.filename = filename;

        String uri = "http://file-svc/files/";
        ClientHttpResponse response = restTemplate.execute(uri + dataId, HttpMethod.GET, null, res -> res);

        saveFileToDisk(response.getBody(), filename);
    }

    private void saveFileToDisk(InputStream file, String filename) throws IOException {
        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }
        assertTrue(new File(specificUploadDir).mkdir());

        File f = new File(specificUploadDir + File.separator + filename);
        System.out.println("File saved to: " + f.getPath());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
        FileCopyUtils.copy(file, stream);
        stream.close();
    }

    public void startImport(String title) throws IOException, FactoryException, GisImportException,
            GisValidationException {
        System.out.println("Starting import: " + title);

        metadataClient.setState(dataId, ImportState.PROCESSING);

        String filename = specificUploadDir + File.separator + this.filename;
        GisValidator gisValidator = new GisValidator(filename);

        filename = gisValidator.getFilename();

        File file = new File(filename);

        publisher = geoServerController.getPublisher();
        if (!publisher.createWorkspace(dataId)) {
            throw new GisImportException("Workspace creation failed!");
        }

        GisType gisType = gisValidator.getGisType();
        String dataName = gisValidator.getShpBasename();

        title = title.replaceAll(" ", "");

        boolean importSuccess = false;

        String crsCode = getCrsCode(gisValidator);

        Map<String, Object> additionalTypeSpecificData = calculateMetadataBounds(gisValidator);


        switch (gisType) {
            case ASC:
                // We converted the Ascii Grid to GeoTiff, so this imports GeoTIFF
            case TIF:
                String baseName = FilenameUtils.getBaseName(title);

                importSuccess = publisher.publishGeoTIFF(dataId, "Webui_Raster", baseName, file, crsCode,
                        GSResourceEncoder.ProjectionPolicy.NONE, "default_point", null);

                additionalTypeSpecificData.put("uri", geoServerExport.generateRasterUri(dataId, title).toString());
                break;
            case SHP:
                importSuccess = publisher.publishShp(dataId, "Webui_Vector", dataName, file, crsCode,
                        "default_point");

                String timeseriesDataId = importTimeseriesData(gisValidator.getTimeseriesFilename(), title);
                if (timeseriesDataId != null) {
                    additionalTypeSpecificData.put("timeseriesDataId", timeseriesDataId);
                }

                additionalTypeSpecificData.put("uri", geoServerExport.generateVectorUri(dataId, dataName).toString());
                break;
        }

        if (!importSuccess) {
            throw new GisImportException("Error inside the GeoServer while importing: " + title);
        }

        writeMetadata(gisType, additionalTypeSpecificData);

        metadataClient.setState(dataId, ImportState.FINISHED);

        System.out.println("Import successful: " + title);
    }

    public void deleteWorkspace(String dataId) throws GisImportException {
        if (!publisher.removeWorkspace(dataId, true)) {
            throw new GisImportException("Deleting workspace for: " + dataId + " failed!");
        }
    }

    public void deleteDirectoryOnDisk() {
        File file = new File(specificUploadDir);

        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            System.out.println("Error, while deleting temporary upload dir: " + specificUploadDir);
        }
    }

    private String getCrsCode(GisValidator gisValidator) throws FactoryException {
        CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
        return CRS.lookupIdentifier(crs, true);
    }

    private Map<String, Object> calculateMetadataBounds(GisValidator gisValidator) {
        Map<String, Double> topLeftBound = new HashMap<>();
        topLeftBound.put("lat", gisValidator.getTopRightBound()[0]);
        topLeftBound.put("lng", gisValidator.getTopRightBound()[1]);

        Map<String, Double> bottomRightBound = new HashMap<>();
        bottomRightBound.put("lat", gisValidator.getBottomLeftBound()[0]);
        bottomRightBound.put("lng", gisValidator.getBottomLeftBound()[1]);

        Map<String, Object> additionalTypeSpecificData = new HashMap<>();
        additionalTypeSpecificData.put("topLeftBound", topLeftBound);
        additionalTypeSpecificData.put("bottomRightBound", bottomRightBound);
        return additionalTypeSpecificData;
    }

    private String importTimeseriesData(String filename, String title) throws FileNotFoundException, GisImportException {
        if (filename.length() < 1) {
            return null;
        }

        System.out.println("Starting timeseries import: " + filename);

        if (!new File(filename).exists()) {
            throw new FileNotFoundException("File does not exist: " + filename);
        }

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", new FileSystemResource(filename));
        parameters.add("privacy", "PUBLIC");
        parameters.add("dataType", "RAW");
        parameters.add("projectId", "1");
        parameters.add("userId", "1");
        parameters.add("title", title);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parameters, headers);

        String url = "http://file-svc/files";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        System.out.println("response: " + response);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new GisImportException("Error during timeseries import: " + response.getBody());
        }

        return response.getBody();
    }

    private void writeMetadata(GisType gisType, Map<String, Object> additionalTypeSpecificData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", gisType.getName());
        metadata.put("additionalTypeSpecificData", additionalTypeSpecificData);

        metadataClient.updateMetadata(dataId, metadata);
    }

}

package org.mars_group.gisimport.service;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.mars_group.core.ImportState;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GisType;
import org.mars_group.gisimport.util.GisValidator;
import org.mars_group.metadataclient.MetadataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Component
public class ImportService {

    private final RestTemplate restTemplate;
    private final MetadataClient metadataClient;
    private String dataId;
    private String specificUploadDir;
    private String filename;

    @Value("${file-svc-url}")
    private String fileServiceUrl;

    @Autowired
    public ImportService(RestTemplate restTemplate, @Value("${file-svc-url}") String fileServiceUrl) {
        this.restTemplate = restTemplate;
        this.metadataClient = new MetadataClient(restTemplate);
        this.fileServiceUrl = fileServiceUrl;
    }

    public void downloadFile(String dataId, String filename) throws IOException {
        System.out.println(filename + ": Downloading file ...");
        createDownloadDirectories(dataId);

        this.dataId = dataId;
        this.filename = specificUploadDir + File.separator + filename;

        String url = fileServiceUrl + "/" + dataId;
        ClientHttpResponse response = restTemplate.execute(url, HttpMethod.GET, null, res -> res);

        File file = new File(this.filename);
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        FileCopyUtils.copy(response.getBody(), stream);
        stream.close();

        assertTrue(file.exists());
    }

    private void createDownloadDirectories(String dataId) {
        String uploadDir = "upload-dir";
        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }

        specificUploadDir = uploadDir + File.separator + dataId;
        assertTrue(new File(specificUploadDir).mkdir());
    }

    public void startImport(String title) throws IOException, GisImportException, GisValidationException {
        System.out.println(title + ": Starting import ...");
        metadataClient.setState(dataId, ImportState.PROCESSING);

        GisValidator gisValidator = new GisValidator(filename, title);

        GisType originalGisType = gisValidator.getOriginalGisType();
        GisType gisType = gisValidator.getGisType();

        Map<String, Object> additionalTypeSpecificData = new HashMap<>();

        if (!originalGisType.equals(gisType)) {
            additionalTypeSpecificData.put("originalType", originalGisType);

            importConvertedFile(gisValidator.getFilename(), title, gisType);
        }

        if (originalGisType.equals(GisType.SHP)) {
            String vectorTsDataId = importVectorTimeseriesData(gisValidator.getTimeseriesFilename(), title);
            if (vectorTsDataId != null) {
                additionalTypeSpecificData.put("timeseriesDataId", vectorTsDataId);
            }
        }

        writeAdditionalTypeSpecificData(gisType, additionalTypeSpecificData);

        System.out.println(title + ": Import successful!");
    }

    private void importConvertedFile(String filename, String title, GisType gisType) throws IOException, GisImportException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException(filename + ": File does not exist!");
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("dataId", dataId);
        builder.addTextBody("dataType", gisType.getName());
        builder.addTextBody("title", title);
        builder.addBinaryBody("file", new FileInputStream(file), ContentType.APPLICATION_OCTET_STREAM, file.getName());


        postRequest(fileServiceUrl + "/replace", builder);
    }

    private String importVectorTimeseriesData(String filename, String title) throws IOException, GisImportException {
        if (filename == null || filename.length() < 1) {
            return null;
        }

        if (!new File(filename).exists()) {
            throw new FileNotFoundException(filename + ": File does not exist!");
        }

        System.out.println(title + ": Starting timeseries import ...");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("privacy", "PUBLIC");
        builder.addTextBody("dataType", "RAW");
        builder.addTextBody("projectId", "1"); // TODO: set real one
        builder.addTextBody("userId", "1"); // TODO: set real one
        builder.addTextBody("title", FilenameUtils.getBaseName(filename) + ".csv");

        File file = new File(filename);
        assertTrue(file.exists());

        builder.addBinaryBody("file", new FileInputStream(file), ContentType.APPLICATION_OCTET_STREAM, file.getName());

        return postRequest(fileServiceUrl, builder).toString();
    }

    private StringBuilder postRequest(String url, MultipartEntityBuilder builder) throws IOException, GisImportException {
        HttpPost httpPost = new HttpPost(url);

        httpPost.setEntity(builder.build());

        CloseableHttpResponse response;
        try {
            response = HttpClients.createDefault().execute(httpPost);
        } catch (HttpClientErrorException e) {
            throw new GisImportException(e.getMessage());
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new GisImportException("Error during import: " + result);
        }
        return result;
    }

    private void writeAdditionalTypeSpecificData(GisType gisType, Map<String, Object> additionalTypeSpecificData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", gisType.getName());
        metadata.put("state", ImportState.FINISHED);

        if (!additionalTypeSpecificData.isEmpty()) {
            metadata.put("additionalTypeSpecificData", additionalTypeSpecificData);
        }

//        for(String key :metadata.keySet()) {
//            System.out.println(key + ": " + metadata.get(key));
//        }
//
//        for (String key : additionalTypeSpecificData.keySet()) {
//            System.out.println(key + ": " + additionalTypeSpecificData.get(key));
//        }

        metadataClient.updateMetadata(dataId, metadata);
    }

    public void setImportToFailed() {
        metadataClient.setState(dataId, ImportState.FAILED);
    }

    public void deleteDirectoryOnDisk() {
        File file = new File(specificUploadDir);

        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            System.out.println("Error, while deleting temporary upload dir: " + specificUploadDir);
        }
    }

}

package org.mars_group.gisimport.import_controller;

import de.haw_hamburg.mars.mars_group.core.ImportState;
import de.haw_hamburg.mars.mars_group.metadataclient.MetadataClient;
import org.apache.commons.io.FileUtils;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.MalformedURLException;

import static org.junit.Assert.assertTrue;

@RestController
class FileUploadController {

    private static final String uploadDir = "upload-dir";

    @Autowired
    GeoServerImport gsImport;

    @Autowired
    RestTemplate restTemplate;

    /**
     * import Geo files
     *
     * @param dataId   generated by file-controller
     * @param title    specified by user
     * @param filename the name of the file, the user uploaded
     * @return status message
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/import")
    public ResponseEntity<String> handleImport(@RequestParam String dataId, @RequestParam String title, @RequestParam String filename) {
        return restTemplate.execute("http://file-service:3333/files/" + dataId, HttpMethod.GET, null, response -> {
            try {
                String specificUploadDir = uploadDir + File.separator + dataId;

                saveFile(response.getBody(), specificUploadDir, filename);
                handleUpload(title, filename, dataId, specificUploadDir);

            } catch (GisImportException e) {
                e.printStackTrace();
                return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>("created", HttpStatus.CREATED);
        });
    }

    private ResponseEntity<String> handleUpload(String title, String filename, String dataId, String specificUploadDir) {

        MetadataClient metadataClient = MetadataClient.getInstance(restTemplate, "http://metadata-service:4444");
        metadataClient.setState(dataId, ImportState.PROCESSING);

        try {
            // START THE IMPORT
            gsImport.handleImport(specificUploadDir, filename, dataId, title);
        } catch (GisImportException | MalformedURLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        metadataClient.setState(dataId, ImportState.FINISHED);

        try {
            FileUtils.deleteDirectory(new File(specificUploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(dataId, HttpStatus.OK);
    }

    private void saveFile(InputStream file, String specificUploadDir, String filename) throws GisImportException {
        try {
            if (!new File(uploadDir).exists()) {
                assertTrue(new File(uploadDir).mkdir());
            }

            if (!new File(specificUploadDir).exists()) {
                assertTrue(new File(specificUploadDir).mkdir());
            }

            // save file
            File f = new File(specificUploadDir + File.separator + filename);
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            FileCopyUtils.copy(file, stream);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GisImportException(e.getMessage());
        }
    }

}

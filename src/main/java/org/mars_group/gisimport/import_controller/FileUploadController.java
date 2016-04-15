package org.mars_group.gisimport.import_controller;

import com.netflix.discovery.EurekaClient;
import de.haw_hamburg.mars.mars_group.core.ImportState;
import de.haw_hamburg.mars.mars_group.core.ImportType;
import de.haw_hamburg.mars.mars_group.core.Privacy;
import de.haw_hamburg.mars.mars_group.metadataclient.MetadataClient;
import org.apache.commons.io.FileUtils;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;


@RestController
class FileUploadController {

    static final String uploadDir = "upload-dir";

    @Autowired
    GeoServerImport gsImport;

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/shp")
    public ResponseEntity<String> handleShpUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String privacy,
            @RequestParam int projectId,
            @RequestParam int userId,
            @RequestParam String title,
            @RequestParam(required = false) String description) {

        return startImport(file, privacy, projectId, userId, title, description, UploadType.SHP);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/asc")
    public ResponseEntity<String> handleAscUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String privacy,
            @RequestParam int projectId,
            @RequestParam int userId,
            @RequestParam String title,
            @RequestParam(required = false) String description) {

        return startImport(file, privacy, projectId, userId, title, description, UploadType.ASC);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/geotiff")
    public ResponseEntity<String> handleGeoTiffUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String privacy,
            @RequestParam int projectId,
            @RequestParam int userId,
            @RequestParam String title,
            @RequestParam(required = false) String description) {

        return startImport(file, privacy, projectId, userId, title, description, UploadType.GEOTIFF);
    }

    private ResponseEntity<String> startImport(MultipartFile file,
                                               String privacy,
                                               int projectId,
                                               int userId,
                                               String title,
                                               String description,
                                               UploadType uploadType) {

        if (!new File(uploadDir).exists()) {
            if (!new File(uploadDir).mkdir()) {
                return new ResponseEntity<>("Failed to create upload dir!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        String error = saveFile(file);
        if (error.length() > 0) {
            cleanUp();
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String result;

        MetadataClient metadataClient = MetadataClient.getInstance(new RestTemplate(), "http://metadata:4444");

        String importId = UUID.randomUUID().toString();
        Privacy privacyType = Privacy.valueOf(privacy);

        boolean initMetaDataSucceeded = metadataClient.initMetaData(
                importId, projectId, userId, privacyType, 42.0, 23.0, ImportType.GIS, title, description);

        if (!initMetaDataSucceeded) {
            System.out.println(importId + " Metadata creation failed");
            cleanUp();
            return new ResponseEntity<>(importId + " Metadata creation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        switch (uploadType) {
            case SHP:
            case ASC:
            case GEOTIFF:
                metadataClient.setState(importId, ImportState.PROCESSING);
                try {
                    result = gsImport.handleImport(file.getOriginalFilename(), uploadType);
                } catch (GisImportException e) {
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
                metadataClient.setState(importId, ImportState.FINISHED);
                break;
            default:
                metadataClient.setState(importId, ImportState.FAILED);
                cleanUp();
                return new ResponseEntity<>("unsupported file type!", HttpStatus.BAD_REQUEST);
        }

        cleanUp();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private String saveFile(MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                // save file
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(
                        new File(uploadDir + "/" + file.getOriginalFilename())));
                FileCopyUtils.copy(file.getInputStream(), stream);
                stream.close();

                return "";

            } catch (Exception e) {
                return "You failed to upload " + file.getOriginalFilename() + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + file.getOriginalFilename() + " because the file was empty";
        }

    }

    private void cleanUp() {
        try {
            FileUtils.deleteDirectory(new File(FileUploadController.uploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

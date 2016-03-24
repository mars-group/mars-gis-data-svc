package org.mars_group.gisimport.import_controller;

import de.haw_hamburg.mars.mars_group.core.ImportState;
import de.haw_hamburg.mars.mars_group.core.ImportType;
import de.haw_hamburg.mars.mars_group.core.Privacy;
import de.haw_hamburg.mars.mars_group.metadataclient.MetadataClient;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
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
import java.util.UUID;


@RestController
public class FileUploadController {

    static final String uploadDir = "upload-dir";

    // create uploadDir
    @Bean
    CommandLineRunner init() {
        return (String[] args) -> new File(uploadDir).mkdir();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/import/shp")
    public
    @ResponseBody
    ResponseEntity<String> handleShpUpload(
            @RequestParam("file") MultipartFile file, @RequestParam String privacy,
            @RequestParam int projectId, @RequestParam int userId, @RequestParam String title,
            @RequestParam(required = false) String description) {

        String error = saveFile(file);
        String result;
        try {
            if (error.length() > 0) {
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String importId = UUID.randomUUID().toString();
            MetadataClient metadataClient = MetadataClient.getInstance(new RestTemplate(), "http://metadata:4444");

            Privacy privacyType = Privacy.valueOf(privacy);

            boolean initMetaDataSucceeded = metadataClient.initMetaData(importId, projectId, userId, privacyType, 42.0, 23.0,
                    ImportType.GIS, title, description);
            if (!initMetaDataSucceeded) {
                System.out.println(importId + " Metadata creation failed");
            }

            metadataClient.setState(importId, ImportState.PROCESSING);

            GeoServerImport gsImport = new GeoServerImport();
            result = gsImport.importShp(file.getOriginalFilename());

            metadataClient.setState(importId, ImportState.FINISHED);
        } finally {
            cleanUp();
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/import/tif")
    public String handleTifUpload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("name") String name) {

        String error = saveFile(file);
        String result;
        try {

            if (error.length() > 0) {
                return error;
            }

            GeoServerImport gsImport = new GeoServerImport();
            result = gsImport.importGeoTiff(file.getOriginalFilename(), name);
        } finally {
            cleanUp();
        }
        return result;
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

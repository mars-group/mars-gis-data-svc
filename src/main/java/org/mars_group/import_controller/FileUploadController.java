package org.mars_group.import_controller;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;


@RestController
public class FileUploadController {

    static final String uploadDir = "upload-dir";

    // create uploadDir
    @Bean
    CommandLineRunner init() {
        return (String[] args) -> new File(uploadDir).mkdir();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/import/shp")
    public String handleShpUpload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("name") String name) {

        String error = saveFile(file, name);
        if (error.length() > 0) {
            return error;
        }

        GeoServerImport gsImport = new GeoServerImport();
        String result = gsImport.importShp(file.getOriginalFilename(), name);

        return result;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/import/tif")
    public String handleTifUpload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("name") String name) {

        String error = saveFile(file, name);
        if (error.length() > 0) {
            return error;
        }

        GeoServerImport gsImport = new GeoServerImport();
        return gsImport.importGeoTiff(file.getOriginalFilename(), name);
    }

    private String saveFile(MultipartFile file, String name) {
        if (!file.isEmpty()) {
            try {
                // save file
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(
                        new File(uploadDir + "/" + file.getOriginalFilename())));
                FileCopyUtils.copy(file.getInputStream(), stream);
                stream.close();

                return "";

            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + name + " because the file was empty";
        }

    }

}

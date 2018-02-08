package org.mars_group.gisimport.web;

import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.service.GeoServerExport;
import org.mars_group.gisimport.service.GeoServerImport;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
class FileController {

    private final GeoServerImport geoServerImport;
    private final GeoServerExport geoServerExport;

    @Autowired
    public FileController(GeoServerImport geoServerImport, GeoServerExport geoServerExport) {
        this.geoServerImport = geoServerImport;
        this.geoServerExport = geoServerExport;
    }

    /**
     * Import Geo files.
     *
     * @param dataId   generated by file-web
     * @param filename the name of the file, the user uploaded
     * @return status message
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/gis")
    public ResponseEntity<String> postFile(@RequestParam String dataId,
                                           @RequestParam String title,
                                           @RequestParam String filename) {
        try {
            geoServerImport.downloadFile(dataId, filename);
            geoServerImport.startImport(title);
        } catch (GisImportException | IOException | FactoryException | GisValidationException e) {
            e.printStackTrace();
            System.out.println("Import failed: " + title);
            try {
                geoServerImport.deleteWorkspace(dataId);
            } catch (GisImportException e1) {
                e1.printStackTrace();
            }
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            geoServerImport.deleteDirectoryOnDisk();
        }

        return new ResponseEntity<>("import successful", HttpStatus.CREATED);
    }

    /**
     * Gets the file url that is used to download the file from the GeoServer
     *
     * @param dataId id created during import
     * @return relative uri to the file
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/gis/{dataId}")
    public ResponseEntity<String> getFileUrl(@PathVariable("dataId") String dataId) {
        return new ResponseEntity<>(geoServerExport.getUriFromDataId(dataId).toString(), HttpStatus.OK);
    }

    /**
     * Delete a resource
     *
     * @param dataId of the desired file
     * @return Success status
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.DELETE, value = "/gis/{dataId}")
    public ResponseEntity<String> deleteFile(@PathVariable("dataId") String dataId) {
        try {
            geoServerImport.deleteWorkspace(dataId);
        } catch (GisImportException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("deleted", HttpStatus.OK);
    }

}

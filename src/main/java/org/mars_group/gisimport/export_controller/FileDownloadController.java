package org.mars_group.gisimport.export_controller;

import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.mars_group.gisimport.util.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


@RestController
public class FileDownloadController {

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/shp")
    public ResponseEntity<String> downloadShp(
            @RequestParam String layername
    ) throws IOException, GisImportException {

        String url;
        try {
            url = buildString(UploadType.SHP, layername);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("error while trying to download: " + layername, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(url, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/geotiff")
    public ResponseEntity<String> downloadGeoTiff(
            @RequestParam String layername
    ) throws IOException, GisImportException {

        String url;
        try {
            url = buildString(UploadType.GEOTIFF, layername);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("error while trying to download: " + layername, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(url, HttpStatus.OK);
    }

    private String buildString(UploadType uploadType, String layername) throws URISyntaxException, MalformedURLException, GisImportException {
        String uri;
        if (uploadType.equals(UploadType.SHP)) {
            uri = UriBuilder.fromUri("/geoserver")
                    .path("wfs")
                    .queryParam("request", "GetFeature")
                    .queryParam("version", "2.0.0")
                    .queryParam("typeName", layername)
                    .queryParam("outputFormat", "shape-zip")
                    .build().toString();
        } else {
            uri = UriBuilder.fromUri("/geoserver")
                    .path("wcs")
                    .queryParam("request", "GetCoverage")
                    .queryParam("service", "WCS")
                    .queryParam("version", "2.0.1")
                    .queryParam("coverageId", layername)
                    .queryParam("format", "geotiff")
                    .build().toString();
        }

        return uri;
    }
}

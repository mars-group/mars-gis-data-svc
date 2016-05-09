package org.mars_group.gisimport.export_controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.UriBuilder;


@RestController
public class FileDownloadController {

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/vector")
    public ResponseEntity<String> downloadShp(@RequestParam String importId,
                                              @RequestParam String layername) {

        String url = UriBuilder.fromUri("/geoserver")
                .path("wfs")
                .queryParam("request", "GetFeature")
                .queryParam("version", "2.0.0")
                .queryParam("typeName", importId + ":" + layername)
                .queryParam("outputFormat", "shape-zip")
                .build().toString();
        return new ResponseEntity<>(url, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/raster")
    public ResponseEntity<String> downloadGeoTiff(@RequestParam String importId,
                                                  @RequestParam String layername) {

        String url = UriBuilder.fromUri("/geoserver")
                .path("wcs")
                .queryParam("request", "GetCoverage")
                .queryParam("service", "WCS")
                .queryParam("version", "2.0.1")
                .queryParam("coverageId", importId + ":" + layername)
                .queryParam("format", "ArcGrid")
                .build().toString();
        return new ResponseEntity<>(url, HttpStatus.OK);
    }
}

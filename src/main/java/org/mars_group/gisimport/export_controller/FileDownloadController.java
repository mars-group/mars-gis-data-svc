package org.mars_group.gisimport.export_controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.UriBuilder;


@RestController
public class FileDownloadController {

    /**
     * Download Vector files like shapefiles
     *
     * @param importId    id created during import
     * @param datasetName the name of the files inside the Zip file without the file extension.
     * @return relative uri to the file
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/vector")
    public ResponseEntity<String> downloadVectorFile(@RequestParam String importId,
                                                     @RequestParam String datasetName) {

        String uri = UriBuilder.fromUri("/geoserver")
                .path("wfs")
                .queryParam("request", "GetFeature")
                .queryParam("version", "2.0.0")
                .queryParam("typeName", importId + ":" + datasetName)
                .queryParam("outputFormat", "shape-zip")
                .build().toString();
        return new ResponseEntity<>(uri, HttpStatus.OK);
    }

    /**
     * Download raster files like AsciiGrid or GeoTiff
     *
     * @param importId id created during import
     * @param title    the title you specified during import
     * @return relative uri to the file
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/raster")
    public ResponseEntity<String> downloadRasterFile(@RequestParam String importId,
                                                     @RequestParam String title) {

        String uri = UriBuilder.fromUri("/geoserver")
                .path("wcs")
                .queryParam("request", "GetCoverage")
                .queryParam("service", "WCS")
                .queryParam("version", "2.0.1")
                .queryParam("coverageId", importId + ":" + title)
                .queryParam("format", "ArcGrid")
                .build().toString();
        return new ResponseEntity<>(uri, HttpStatus.OK);
    }
}

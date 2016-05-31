package org.mars_group.gisimport.export_controller;

import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.UriBuilder;


@RestController
public class FileDownloadController {

    /**
     * Download Vector files like shapefiles
     *
     * @param dataId   id created during import
     * @param dataName the name of the files inside the Zip file without the file extension.
     * @return relative uri to the file
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/vector")
    public String downloadVectorFile(@RequestParam String dataId, @RequestParam String dataName) {

        return UriBuilder.fromUri("")
                .path("wfs")
                .queryParam("request", "GetFeature")
                .queryParam("version", "2.0.0")
                .queryParam("typeName", dataId + ":" + dataName)
                .queryParam("outputFormat", "shape-zip")
                .build().toString();
    }

    /**
     * Download raster files like AsciiGrid or GeoTiff
     *
     * @param dataId id created during import
     * @param title  the title you specified during import
     * @return relative uri to the file
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/raster")
    public String downloadRasterFile(@RequestParam String dataId, @RequestParam String title) {

        return UriBuilder.fromUri("")
                .path("wcs")
                .queryParam("request", "GetCoverage")
                .queryParam("service", "WCS")
                .queryParam("version", "2.0.1")
                .queryParam("coverageId", dataId + ":" + title)
                .queryParam("format", "ArcGrid")
                .build().toString();
    }
}

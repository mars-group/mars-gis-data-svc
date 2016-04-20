package org.mars_group.gisimport.export_controller;

import de.haw_hamburg.mars.mars_group.core.ImportType;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServerInstance;
import org.mars_group.gisimport.util.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;


@RestController
public class FileDownloadController {

    @Autowired
    GeoServerInstance geoServerInstance;

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/shp")
    public ResponseEntity<String> downloadShp(
            @RequestParam String layername
    ) throws IOException, GisImportException {

//        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

//        HttpHeaders headers = new HttpHeaders();
//        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

        String url;
        try {
            url = buildString(UploadType.SHP, layername);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("error while trying to download: " + layername, HttpStatus.INTERNAL_SERVER_ERROR);
        }

//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class, "1");
//
//        System.out.println("response code: " + response.getStatusCode());
//        if (response.getStatusCode() == HttpStatus.OK) {
//            Files.write(Paths.get("shape.zip"), response.getBody());
//
//            return new ResponseEntity<>(HttpStatus.OK);
//        }

        return new ResponseEntity<>(url, HttpStatus.OK);

//        return new ResponseEntity<>("error while trying to download: " + layername, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/download/geotiff")
    public ResponseEntity<String> downloadGeoTiff(
            @RequestParam String layername
    ) throws IOException, GisImportException {

        System.out.println("Layername: " + layername);

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
        if(uploadType.equals(UploadType.SHP)) {
            uri = UriBuilder.fromUri(geoServerInstance.getUri())
                    .path("wfs")
                    .queryParam("request", "GetFeature")
                    .queryParam("version", "2.0.0")
                    .queryParam("typeName", layername)
                    .queryParam("outputFormat", "shape-zip")
                    .build().toString();
        } else {
            uri = UriBuilder.fromUri(geoServerInstance.getUri())
                    .path("wcs")
                    .queryParam("request", "GetCoverage")
                    .queryParam("service", "WCS")
                    .queryParam("version", "2.0.1")
                    .queryParam("coverageId", layername)
                    .queryParam("format", "geotiff")
                    .build().toString();
        }

        System.out.println("URI: " + uri);

        return uri;
    }
}

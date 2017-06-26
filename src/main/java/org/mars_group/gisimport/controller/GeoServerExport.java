package org.mars_group.gisimport.controller;

import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.mars_group.core.Metadata;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServer;
import org.mars_group.metadataclient.MetadataClient;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.ws.rs.core.UriBuilder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

@Component
class GeoServerExport {

    private final GeoServer geoServer;
    private final MetadataClient metadataClient;
    private NumberFormat formatter;

    @Autowired
    public GeoServerExport(RestTemplate restTemplate, GeoServer geoServer) {
        this.geoServer = geoServer;
        this.metadataClient = new MetadataClient(restTemplate);
        formatter = NumberFormat.getInstance(new Locale("de_DE"));
    }

    URI createRasterUri(String dataId, String title) throws MalformedURLException, GisImportException {
        GeoServerRESTReader reader = geoServer.getReader();
        RESTLayer layer = reader.getLayer(dataId, title);

        if (layer == null) {
            System.out.println("Layer is null");
            return URI.create("");
        }

        return UriBuilder.fromUri("")
                .path("wcs")
                .queryParam("request", "GetCoverage")
                .queryParam("service", "WCS")
                .queryParam("version", "2.0.1")
                .queryParam("coverageId", dataId + ":" + title)
                .queryParam("format", "GeoTIFF")
                .build();
    }

    URI createVectorUri(String dataId, String dataName) throws MalformedURLException, GisImportException {
        return UriBuilder.fromUri("")
                .path("wfs")
                .queryParam("request", "GetFeature")
                .queryParam("version", "2.0.0")
                .queryParam("typeName", dataId + ":" + dataName)
                .queryParam("outputFormat", "shape-zip")
                .build();
    }

    URI getUriFromDataId(String dataId) throws MalformedURLException, GisImportException {
        Metadata metadata = metadataClient.getMetadata(dataId);
        Map typeSpecificFields = metadata.getAdditionalTypeSpecificData();

        return URI.create(typeSpecificFields.get("uri").toString());
    }

    double getValue(String dataId, Point2D gps) throws IOException, GisImportException, TransformException {
        // ToDo: handle vector file.

        File file = downloadFile(dataId);

        return readPixelValue(file, convertGpsToPixels(gps));
    }

    private File downloadFile(String dataId) throws IOException, GisImportException {
        URL url = new URL(geoServer.getBaseUrl() + "/" + getUriFromDataId(dataId));

        File file = new File("tmpFile.tif");
        FileUtils.copyURLToFile(url, file);

        return file;
    }

    // TODO: implement for real
    private Point convertGpsToPixels(Point2D gps) {
        System.out.println("gps: " + gps);

        Random rnd = new Random();

        Point pixel = new Point(rnd.nextInt(100), rnd.nextInt(100));

        System.out.println("Pixel: " + pixel);

        return pixel;
    }

    private double readPixelValue(File file, Point pixels) throws IOException, TransformException {
        long startTime = System.nanoTime();
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of data at once from the disk...
        ParameterValue<String> gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);

        GridCoverage2DReader reader = new GeoTiffReader(file);

        GridEnvelope dimensions = reader.getOriginalGridRange();
        GridCoordinates maxDimensions = dimensions.getHigh();
        int w = maxDimensions.getCoordinateValue(0) + 1;
        int h = maxDimensions.getCoordinateValue(1) + 1;
        System.out.println("[width, height]: " + w + ", " + h);

        GridCoverage2D coverage = reader.read(new GeneralParameterValue[]{policy, gridSize, useJaiRead});

        RenderedImage image = coverage.getRenderedImage();
        RandomIter iterator = RandomIterFactory.create(image, null);
        int value = iterator.getSample(pixels.x, pixels.y, 0);

        System.out.println("total: " + formatter.format(System.nanoTime() - startTime) + " nanoseconds");

        return value;
    }

    private void readRandomValues(File file, int n) throws IOException, TransformException {
        long startTime = System.nanoTime();
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of data at once from the disk...
        ParameterValue<String> gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);

        GridCoverage2DReader reader = new GeoTiffReader(file);
        GridEnvelope dimensions = reader.getOriginalGridRange();
        GridCoordinates maxDimensions = dimensions.getHigh();
        int w = maxDimensions.getCoordinateValue(0) + 1;
        int h = maxDimensions.getCoordinateValue(1) + 1;
        int numBands = reader.getGridCoverageCount();

        GridCoverage2D coverage = reader.read(new GeneralParameterValue[]{policy, gridSize, useJaiRead});
//        GridGeometry2D geometry = coverage.getGridGeometry();

        Random rnd = new Random();
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(w - 1);
            int y = rnd.nextInt(h - 1);
            points.add(new Point(x, y));
        }

//        System.out.println("[width, height]: " + w + ", " + h);

        for (Point point : points) {
            int x = point.x;
            int y = point.y;
//            System.out.println("[x, y]: " + x + ", " + y);

//            Envelope2D pixelEnvelop = geometry.gridToWorld(new GridEnvelope2D(x, y, 1, 1));
//            double lon = pixelEnvelop.getCenterX();
//            double lat = pixelEnvelop.getCenterY();
//            System.out.println("[lon, lat]: " + lon + ", " + lat);

            double[] vals = new double[numBands];
            coverage.evaluate(new GridCoordinates2D(x, y), vals);

            //Do something!
//            for (double val : vals) {
//                System.out.println(val);
//            }
        }
        long time = System.nanoTime() - startTime;
        System.out.println(formatter.format(n) + " runs.");
        System.out.println("total: " + formatter.format(time / 1000) + " microseconds");
        System.out.println("average: " + formatter.format(time / n / 1000) + " microseconds");
    }

}

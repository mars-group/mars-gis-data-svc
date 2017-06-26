package org.mars_group.gisimport.controller;

import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.mars_group.core.Metadata;
import org.mars_group.gisimport.exceptions.GisExportException;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.util.GeoServer;
import org.mars_group.gisimport.util.UnzipUtility;
import org.mars_group.metadataclient.MetadataClient;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
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
import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Component
class GeoServerExport {

    private final GeoServer geoServer;
    private final MetadataClient metadataClient;

    @Autowired
    public GeoServerExport(RestTemplate restTemplate, GeoServer geoServer) {
        this.geoServer = geoServer;
        this.metadataClient = new MetadataClient(restTemplate);
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

    String getValue(String dataId, Point2D gps) throws IOException, GisImportException, TransformException, GisExportException, CQLException {
        Metadata metadata = metadataClient.getMetadata(dataId);

        String filename = dataId;
        switch (metadata.getType()) {
            case ASCIIGRID:
            case GEOTIFF:
                filename += ".tif";
                return readRasterValue(downloadFile(filename, dataId), convertGpsToPixels(gps));
            case SHAPEFILE:
                filename += ".zip";
                String dataName = metadata.getAdditionalTypeSpecificData().get("dataName").toString();
                return readVectorValue(downloadFile(filename, dataId), dataName);
            default:
                throw new GisExportException("wrong file type!");
        }
    }

    private String downloadFile(String filename, String dataId) throws IOException, GisImportException {
        File file = new File(filename);
        if (file.exists()) {
            return filename;
        }

        URL url = new URL(geoServer.getBaseUrl() + "/" + getUriFromDataId(dataId));

        System.out.println("File does not exist: " + filename + " I will download it from: " + url);

        FileUtils.copyURLToFile(url, file);

        return filename;
    }

    // TODO: implement for real
    private Point convertGpsToPixels(Point2D gps) {
//        System.out.println("gps: " + gps);

        Random rnd = new Random();

        Point pixel = new Point(rnd.nextInt(400), rnd.nextInt(400));

//        System.out.println("Pixel: " + pixel);

        return pixel;
    }

    private String readRasterValue(String filename, Point pixels) throws IOException, TransformException {
        File file = new File(filename);

        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of data at once from the disk...
        ParameterValue<String> gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
//        useJaiRead.setValue(true);

        GridCoverage2DReader reader = new GeoTiffReader(file);

        GridCoverage2D coverage = reader.read(new GeneralParameterValue[]{policy, gridSize, useJaiRead});

        RenderedImage image = coverage.getRenderedImage();
        RandomIter iterator = RandomIterFactory.create(image, null);

        return String.valueOf(iterator.getSample(pixels.x, pixels.y, 0));
    }

    private String readVectorValue(String zipFilename, String dataName) throws IOException, CQLException {
        UnzipUtility unzipper = new UnzipUtility();
        unzipper.unzip(zipFilename, ".");

        File file = new File(dataName + ".shp");

        Map map = Collections.singletonMap("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = ECQL.toFilter("NAME = 'Germany'");

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            SimpleFeature feature = features.next();
//            System.out.println(feature.getDefaultGeometryProperty().getValue());
            return feature.getID();
        }

    }

}

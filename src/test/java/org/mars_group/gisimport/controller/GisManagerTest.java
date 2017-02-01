package org.mars_group.gisimport.controller;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GisManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GisManagerTest {

    private final static String uploadDir = "upload-dir";

    @BeforeClass
    public static void createUploadDir() {
        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }
    }

    @Test
    public void asciiGridTest() {
        asciiGridTest("src/test/resources/ascii_grid.asc");
    }

    @Test
    public void asciiGridPeriodTest() {
        asciiGridTest("src/test/resources/ascii_grid.period.asc");
    }

    @Test
    public void asciiGridSpaceTest() {
        asciiGridTest("src/test/resources/ascii_grid space.asc");
    }

    @Test
    public void asciiGridZipTest() {
        asciiGridTest("src/test/resources/ascii_grid.zip");
    }

    @Test
    public void asciiGridZipPeriodTest() {
        asciiGridTest("src/test/resources/ascii_grid.period.zip");
    }

    private void asciiGridTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisManager gisManager = new GisManager(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisManager.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NZGD49 / New Zealand Map Grid"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void geoTiffTest() {
        geoTiffTest("src/test/resources/geotiff.tif");
    }

    @Test
    public void geoTiffZipTest() {
        geoTiffTest("src/test/resources/geotiff.zip");
    }

    @Test
    public void geoTiffPeriodTest() {
        geoTiffTest("src/test/resources/geotiff.period.tif");
    }

    @Test
    public void geoTiffSpaceTest() {
        geoTiffTest("src/test/resources/geotiff space.tif");
    }

    @Test
    public void geoTiffZipPeriodTest() {
        geoTiffTest("src/test/resources/geotiff.period.zip");
    }

    private void geoTiffTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisManager gisManager = new GisManager(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisManager.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void shpTest() {
        shpPeriodTest("src/test/resources/shapefile.zip");
    }

    @Test
    public void shpSpaceTest() {
        shpPeriodTest("src/test/resources/shapefile space.zip");
    }

    @Test
    public void shpDirectoryTest() {
        shpPeriodTest("src/test/resources/shapefileDirectory.zip");
    }

    @Test
    public void shpCapitalExtensionTest() {
        shpPeriodTest("src/test/resources/shapefileCapitalExtension.ZIP");
    }

    @Test
    public void shpOtherFileInsideTest() {
        shpPeriodTest("src/test/resources/shapefileOtherFileInside.zip");
    }

    private void shpPeriodTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisManager gisManager = new GisManager(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisManager.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));
            assertTrue(gisManager.getDataName().equals("pop_pnt"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        }
    }

    private String createSpecificUploadDir() {
        String dataId = UUID.randomUUID().toString();
        String localUploadDir = uploadDir + File.separator + dataId;

        if (!new File(localUploadDir).exists()) {
            assertTrue(new File(localUploadDir).mkdir());
        }

        return localUploadDir;
    }

    @AfterClass
    public static void cleanUp() {
        try {
            FileUtils.deleteDirectory(new File(uploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

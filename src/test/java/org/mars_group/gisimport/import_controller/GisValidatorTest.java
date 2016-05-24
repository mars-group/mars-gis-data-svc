package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GisValidatorTest {

    private final static String uploadDir = "upload-dir";

    @BeforeClass
    public static void createUploadDir() {
        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }
    }

    @Test
    public void asciiGridTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.asc";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridPeriodTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.period.asc";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridSpaceTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid space.asc";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridZipTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.zip";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridZipPeriodTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.period.zip";
        asciiGridTest(filename);
    }

    private void asciiGridTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NZGD49 / New Zealand Map Grid"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void geoTiffTest() {
        String filename = "src/test/resources/geotiff.tif";
        geoTiffTest(filename);
    }

    @Test
    public void geoTiffZipTest() {
        String filename = "src/test/resources/geotiff.zip";
        geoTiffTest(filename);
    }

    @Test
    public void geoTiffPeriodTest() {
        String filename = "src/test/resources/geotiff.period.tif";
        geoTiffTest(filename);
    }

    @Test
    public void geoTiffSpaceTest() {
        String filename = "src/test/resources/geotiff space.tif";
        geoTiffTest(filename);
    }

    @Test
    public void geoTiffZipPeriodTest() {
        String filename = "src/test/resources/geotiff.period.zip";
        geoTiffTest(filename);
    }

    private void geoTiffTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void shpTest() {
        String filename = "src/test/resources/shapefile.zip";
        shpPeriodTest(filename);
    }

    @Test
    public void shpSpaceTest() {
        String filename = "src/test/resources/shapefile space.zip";
        shpPeriodTest(filename);
    }

    private void shpPeriodTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createSpecificUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));
            assertTrue(gisValidator.getDatasetName().equals("pop_pnt"));

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
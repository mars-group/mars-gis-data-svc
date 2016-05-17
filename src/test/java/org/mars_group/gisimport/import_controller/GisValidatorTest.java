package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

public class GisValidatorTest {

    private String uploadDir;

    public GisValidatorTest() {
        String importId = UUID.randomUUID().toString();
        uploadDir = "upload-dir";

        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }

        uploadDir += File.separator + importId;
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
    public void asciiGridPeriodeTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.period.asc";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridZipTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.zip";
        asciiGridTest(filename);
    }

    @Test
    public void asciiGridZipPeriodeTest() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.period.zip";
        asciiGridTest(filename);
    }

    private void asciiGridTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(uploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NZGD49 / New Zealand Map Grid"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
            assertFalse(new File(uploadDir).exists());
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
    public void geoTiffPeriodeTest() {
        String filename = "src/test/resources/geotiff.period.tif";
        geoTiffTest(filename);
    }

    @Test
    public void geoTiffZipPeriodeTest() {
        String filename = "src/test/resources/geotiff.period.zip";
        geoTiffTest(filename);
    }

    private void geoTiffTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(uploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
            assertFalse(new File(uploadDir).exists());
        }
    }

    @Test
    public void shpTest() {
        String filename = "src/test/resources/shapefile.zip";
        shpPeriodTest(filename);
    }

    private void shpPeriodTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(uploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));
            assertTrue(gisValidator.getDatasetName().equals("pop_pnt"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
            assertFalse(new File(uploadDir).exists());
        }
    }

    private void cleanUp() {
        try {
            FileUtils.deleteDirectory(new File(uploadDir));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

}
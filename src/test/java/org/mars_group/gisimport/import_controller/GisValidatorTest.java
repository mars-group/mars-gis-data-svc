package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GisValidatorTest {

    private final String uploadDir = "upload-dir";

    public GisValidatorTest() {
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

        String localUploadDir = createUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NZGD49 / New Zealand Map Grid"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp(localUploadDir);
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

        String localUploadDir = createUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp(localUploadDir);
        }
    }

    @Test
    public void shpTest() {
        String filename = "src/test/resources/shapefile.zip";
        shpPeriodTest(filename);
    }

    private void shpPeriodTest(String filename) {
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        String localUploadDir = createUploadDir();

        try {
            GisValidator gisValidator = new GisValidator(localUploadDir, filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));
            assertTrue(gisValidator.getDatasetName().equals("pop_pnt"));

        } catch (IOException | GisValidationException | GisImportException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp(localUploadDir);
        }
    }

    private String createUploadDir() {
        String dataId = UUID.randomUUID().toString();
        String localUploadDir = uploadDir + File.separator + dataId;

        if (!new File(localUploadDir).exists()) {
            assertTrue(new File(localUploadDir).mkdir());
        }

        return localUploadDir;
    }

    private void cleanUp(String directory) {

        // list dirs for debuging only
        File[] files = new File(uploadDir).listFiles();
        System.out.println("///////////////////////////////////////////////////////");
        if (files != null) {
            System.out.println(files.length + " files found!");
            for (File f : files) {
                if (f.isDirectory()) {
                    System.out.println(f.getName());
                }
            }
        }

        try {
            FileUtils.deleteDirectory(new File(directory));
            assertFalse(new File(directory).exists());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
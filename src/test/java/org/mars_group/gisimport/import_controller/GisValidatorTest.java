package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UploadType;
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
    public void shpConstructor() {
        String filename = "src/test/resources/shapefile.zip";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());
        GisValidator gisValidator = new GisValidator(uploadDir, filename, UploadType.SHP);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));
            assertTrue(gisValidator.getDatasetName().equals("pop_pnt"));

        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
            assertFalse(new File(uploadDir).exists());
        }
    }

    @Test
    public void arcGridConstructor() throws FactoryException {
        String filename = "src/test/resources/ascii_grid.asc";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        GisValidator gisValidator = new GisValidator(uploadDir, filename, UploadType.ASC);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NZGD49 / New Zealand Map Grid"));

        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
            assertFalse(new File(uploadDir).exists());
        }
    }

    @Test
    public void geoTiffConstructor() {
        String filename = "src/test/resources/geotiff.tif";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());
        GisValidator gisValidator = new GisValidator(uploadDir, filename, UploadType.GEOTIFF);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

        } catch (IOException | GisValidationException e) {
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
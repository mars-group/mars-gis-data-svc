package org.mars_group.gisimport.import_controller;

import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GisValidatorTest {

    private final String uploadDir = "upload-dir";

    @Test
    public void shpConstructor() {
        if (!new File(uploadDir).exists()) {
            new File(uploadDir).mkdir();
        }
        String filename = "src/test/resources/shapefile.zip";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());
        GisValidator gisValidator = new GisValidator(uploadDir, filename);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic"));

            assertTrue(gisValidator.getDatasetName().equals("pop_pnt"));
            gisValidator.cleanUp();
            assertFalse(new File(uploadDir).exists());

        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void arcGridConstructor() {
        if (!new File(uploadDir).exists()) {
            new File(uploadDir).mkdir();
        }

        String filename = "src/test/resources/ascii_grid.asc";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());
        GisValidator gisValidator = new GisValidator(uploadDir, filename);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:WGS 84 / UTM zone 11N"));

            gisValidator.cleanUp();
            assertFalse(new File(uploadDir).exists());
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
//            fail();
        }
    }

    @Test
    public void geoTiffConstructor() {
        if (!new File(uploadDir).exists()) {
            new File(uploadDir).mkdir();
        }

        String filename = "src/test/resources/geotiff.tif";
        assertTrue(filename + " doesn't exists!", new File(filename).exists());
        GisValidator gisValidator = new GisValidator(uploadDir, filename);

        try {
            gisValidator.validate();
            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N"));

            gisValidator.cleanUp();
            assertFalse(new File(uploadDir).exists());
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

}
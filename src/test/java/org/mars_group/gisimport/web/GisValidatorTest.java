package org.mars_group.gisimport.web;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GisValidator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GisValidatorTest {

    private final static String resourceDir = "src" + File.separator + "test" + File.separator + "resources";
    private final static String uploadDir = "upload-dir";

    private final ArrayList<String> asciiFiles = new ArrayList<>(Arrays.asList(
            "ascii_grid.asc",
            "ascii_grid.period.asc",
            "ascii_grid space.asc",
            "ascii_grid.zip",
            "ascii_grid.period.zip"));

    private final ArrayList<String> geoTiffFiles = new ArrayList<>(Arrays.asList(
            "geotiff.tif",
            "geotiff.zip",
            "geotiff.period.tif",
            "geotiff space.tif",
            "geotiff.period.zip"
    ));

    private final ArrayList<String> shapeFiles = new ArrayList<>(Arrays.asList(
            "shapefile.zip",
            "shapefile space.zip",
            "shapefileDirectory.zip",
            "shapefileCapitalExtension.ZIP",
            "shapefileOtherFileInside.zip",
            "shapefile_no_crs.zip"
    ));

    @BeforeClass
    public static void createUploadDir() {
        if (!new File(uploadDir).exists()) {
            assertTrue(new File(uploadDir).mkdir());
        }
    }

    @AfterClass
    public static void cleanUp() {
        try {
            FileUtils.deleteDirectory(new File(uploadDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void asciiGridTest() {
        for (String filename : asciiFiles) {
            asciiGridTest(filename);
        }
    }

    @Test
    public void geoTiffTest() {
        for (String filename : geoTiffFiles) {
            geoTiffTest(filename);
        }
    }

    @Test
    public void shpTest() {
        for (String filename : shapeFiles) {
            shapeFileTest(filename);
        }
    }

    private void asciiGridTest(String filename) {
        filename = copyFileToUploadDir(filename);
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:WGS 84"));

        } catch (IOException | GisValidationException | FactoryException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void geoTiffTest(String filename) {
        filename = copyFileToUploadDir(filename);
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("EPSG:NAD27 / UTM zone 13N")
                    || crs.getName().toString().equals("EPSG:WGS 84"));

        } catch (IOException | GisValidationException | FactoryException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void shapeFileTest(String filename) {
        filename = copyFileToUploadDir(filename);
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(filename);

            CoordinateReferenceSystem crs = gisValidator.getCoordinateReferenceSystem();
            assertTrue(crs.getName().toString().equals("Geographic")
                    || crs.getName().toString().equals("EPSG:WGS 84"));
            assertTrue(gisValidator.getShpBasename().equals("pop_pnt"));

        } catch (IOException | GisValidationException | FactoryException e) {
            e.printStackTrace();
            fail();
        }
    }

    private String copyFileToUploadDir(String filename) {
        File file = new File(resourceDir + File.separator + filename);

        UUID uuid = UUID.randomUUID();
        assertTrue(new File(uploadDir + File.separator + uuid).mkdir());

        String newFilename = uploadDir + File.separator + uuid + File.separator + file.getName();
        try {
            Files.copy(file.toPath(), Paths.get(newFilename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newFilename;
    }
}

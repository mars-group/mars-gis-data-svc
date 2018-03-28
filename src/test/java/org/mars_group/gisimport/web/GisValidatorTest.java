package org.mars_group.gisimport.web;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.GisValidator;

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

    private final ArrayList<String> rasterFiles = new ArrayList<>(Arrays.asList(
            "ascii_grid.asc",
            "ascii_grid.period.asc",
            "ascii_grid space.asc",
            "ascii_grid.zip",
            "ascii_grid.period.zip",
            "geotiff.tif",
            "geotiff.zip",
            "geotiff.period.tif",
            "geotiff space.tif",
            "geotiff.period.zip"
    ));

    private final ArrayList<String> vectorFiles = new ArrayList<>(Arrays.asList(
            "geoJson.json",
            "shapefile.zip",
            "shapefile space.zip",
            "shapefileDirectory.zip",
            "shapefileCapitalExtension.ZIP",
            "shapefileOtherFileInside.zip",
            "shapefile_no_crs.zip",
            "shapefile_csv.zip"
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
    public void rasterTest() {
        for (String filename : rasterFiles) {
            rasterTest(filename);
        }
    }

    @Test
    public void vectorTest() {
        for (String filename : vectorFiles) {
            vectorTest(filename);
        }
    }

    private void rasterTest(String filename) {
        filename = copyFileToUploadDir(filename);
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(filename, filename);
        } catch (IOException | GisValidationException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void vectorTest(String filename) {
        filename = copyFileToUploadDir(filename);
        System.out.println(filename);
        assertTrue(filename + " doesn't exists!", new File(filename).exists());

        try {
            GisValidator gisValidator = new GisValidator(filename, filename);

            if (filename.equals("shapefile_csv.zip")) {
                String csvFilename = FilenameUtils.getPath(filename) + File.separator + "ipcc_precipitation_gis.csv";
                assertTrue(new File(csvFilename).exists());
            }

        } catch (IOException | GisValidationException e) {
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

package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UnzipUtility;
import org.mars_group.gisimport.util.ZipWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


class GisValidator {
    private boolean zipHasDirectory;
    private String uploadDir;
    private String filename;
    private String datasetDirectoryPath;
    private String datasetDirectoryName;
    private String datasetName;
    private CoordinateReferenceSystem coordinateReferenceSystem;


    /**
     * Validates your GIS file
     *
     * @param uploadDir the upload directory
     * @param filename  this has to be either .zip .shp or .asc
     */
    GisValidator(String uploadDir, String filename) {
        this.filename = filename;
        this.uploadDir = uploadDir;
    }

    void validate() throws GisValidationException, IOException {
        datasetDirectoryName = FilenameUtils.getBaseName(filename);
        String fileExtension = FilenameUtils.getExtension(filename);

        if (fileExtension.equalsIgnoreCase("zip")) {
            zipHasDirectory = zipHasDirectory(filename);

            datasetDirectoryPath = unzip(filename);
            findShpDatasetName();

            if (zipHasDirectory) {
                removeDirectoryFromZip();
            }
            File file = new File(datasetDirectoryPath + "/" + datasetName + ".shp");
            coordinateReferenceSystem = initShpFile(file);

        } else if (fileExtension.equalsIgnoreCase("shp")) {
            // TODO: implement
            throw new GisValidationException(fileExtension + " not implemented yet.");

        } else if (fileExtension.equalsIgnoreCase("asc")) {
            System.out.println("filename: " + filename);

//            coordinateReferenceSystem = initRasterFile(new ArcGridReader(filename));
            datasetName = datasetDirectoryName;

        } else if(fileExtension.equalsIgnoreCase("tif")) {
            coordinateReferenceSystem = initRasterFile(new GeoTiffReader(filename));
            datasetName = datasetDirectoryName;

        } else {
            throw new GisValidationException(fileExtension + " is not a supported file extension!");
        }
    }

    // GeoServer can not handle directories
    private boolean zipHasDirectory(String filename) throws IOException {
        ZipFile zipFile;
        zipFile = new ZipFile(filename);

        Enumeration zipEntries = zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry zip = (ZipEntry) zipEntries.nextElement();
            if (zip.isDirectory() && !zip.getName().equals("__MACOSX/")) {
                System.out.println("Directory: " + zip.getName());
                return true;
            }
        }
        return false;
    }

    private String unzip(String filename) throws IOException {
        String unzipDirectory = uploadDir;

        if (!zipHasDirectory) {
            unzipDirectory += "/" + datasetDirectoryName;
        }

        UnzipUtility unzipper = new UnzipUtility();
        unzipper.unzip(filename, unzipDirectory);

        return unzipDirectory;
    }

    private void removeDirectoryFromZip() {
        ZipWriter zw = new ZipWriter();
        String path = datasetDirectoryPath + "/" + datasetDirectoryName;
        zw.createZip(path, path + ".zip");
    }

    private void findShpDatasetName() {
        datasetName = datasetDirectoryName;
        File folder = new File(datasetDirectoryPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("shp")) {
                    datasetName = FilenameUtils.getBaseName(f.getName());
                    break;
                }
            }
        }
    }

    /**
     * reads the datasetName and spatial reference
     *
     * @param file input file
     */
    private CoordinateReferenceSystem initShpFile(File file) throws IOException {
        Map<String, Object> map = new HashMap<>();

        map.put("url", file.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);

        datasetName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(datasetName);

        return source.getInfo().getCRS();

    }

    /**
     * reads the spatial reference
     *
     * @param reader Reader of the appropriate GIS format
     */
    <T extends GridCoverage2DReader> CoordinateReferenceSystem initRasterFile(T reader) throws IOException {
        GridCoverage2D coverage = reader.read(null);

        return coverage.getCoordinateReferenceSystem2D();
    }

    void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File(uploadDir));
    }

    String getDatasetName() {
        return datasetName;
    }

    CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

}

package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UnzipUtility;
import org.mars_group.gisimport.util.ZipWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class GisValidator {
    private boolean zipHasDirectory;
    private String datasetDirectoryPath;
    private String datasetDirectoryName;
    private String datasetName;
    private String spatialReferenceSystem;

    /**
     * Validates your GIS file
     * @param filename this has to be either .zip .shp or .asc
     * @throws GisValidationException
     */
    public GisValidator(String filename) throws GisValidationException {
        datasetDirectoryName = FilenameUtils.getBaseName(filename);
        String fileExtension = FilenameUtils.getExtension(filename);

        File file;
        if (fileExtension.equalsIgnoreCase("zip")) {
            zipHasDirectory = zipHasDirectory(filename);

            datasetDirectoryPath = unzip(filename);
            findDatasetName();
            file = new File(datasetDirectoryPath + "/" + datasetName + ".shp");

            if (zipHasDirectory) {
                removeDirectoryFromZip();
            }
        } else if(fileExtension.equalsIgnoreCase("shp")) {
            datasetDirectoryPath = FileUploadController.uploadDir;
            findDatasetName();
            file = new File(filename);

            file = new File(datasetDirectoryPath + "/" + datasetName + ".shp");

        } else if(fileExtension.equalsIgnoreCase("asc")) {
            // TODO: implement
            throw new GisValidationException("not implemented yet.");
        } else {
            throw new GisValidationException(fileExtension + " is not a supported file extention");
        }

        initGisFile(file);
    }

    // GeoServer can not handle directories
    private boolean zipHasDirectory(String filename) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (zipFile != null) {
            Enumeration zipEntries = zipFile.entries();

            while (zipEntries.hasMoreElements()) {
                ZipEntry zip = (ZipEntry) zipEntries.nextElement();
                if (zip.isDirectory()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String unzip(String filename) {
        String unzipDirectory = FileUploadController.uploadDir;

        if(!zipHasDirectory) {
            unzipDirectory += "/" + datasetDirectoryName;
        }

        UnzipUtility unzipper = new UnzipUtility();
        try {
            unzipper.unzip(filename, unzipDirectory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return unzipDirectory;
    }

    private void removeDirectoryFromZip() {
        ZipWriter zw = new ZipWriter();
        String path =  datasetDirectoryPath + "/" + datasetDirectoryName;
        zw.createZip(path, path + ".zip");
    }

    private void findDatasetName() {
        datasetName = datasetDirectoryName;
        File folder = new File(datasetDirectoryPath);
        File[] files = folder.listFiles();
        if(files != null) {
            for (File f : files) {
                if (FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("shp")) {
                    datasetName = FilenameUtils.getBaseName(f.getName());
                    break;
                }
            }
        }
    }

    private void initGisFile(File file) {
        Map<String, Object> map = new HashMap<>();

        try {
            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            datasetName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(datasetName);

            spatialReferenceSystem = source.getInfo().getCRS().getCoordinateSystem().getName().getCode();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getSpatialReferenceSystem() {
        return spatialReferenceSystem;
    }

}

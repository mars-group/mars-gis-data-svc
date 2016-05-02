package org.mars_group.gisimport.import_controller;

import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.mars_group.gisimport.util.UnzipUtility;
import org.mars_group.gisimport.util.UploadType;
import org.mars_group.gisimport.util.ZipWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
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
    private UploadType uploadType;
    private String datasetDirectoryPath;
    private String datasetDirectoryName;
    private String datasetName;
    private CoordinateReferenceSystem coordinateReferenceSystem;


    /**
     * Validates your GIS file
     *
     * @param uploadDir  the upload directory
     * @param filename   this has to be either .zip .tif or .asc
     */
    GisValidator(String uploadDir, String filename) throws IOException, GisValidationException, GisImportException {
        this.uploadDir = uploadDir;
        this.uploadType = determinUploadType(filename);

        datasetDirectoryName = FilenameUtils.getBaseName(filename);
        String fileExtension = FilenameUtils.getExtension(filename);

        switch (fileExtension) {
            case "asc":
                validateAsc(filename);
                break;

            case "tif":
                validateGeoTiff(filename);
                break;

            case "zip":
                zipHasDirectory = zipHasDirectory(filename);
                datasetDirectoryPath = unzip(filename);

                switch (this.uploadType) {
                    case ASC:
                        validateAsc(datasetDirectoryPath + File.separator + datasetDirectoryName + ".asc");
                        break;
                    case GEOTIFF:
                        validateGeoTiff(datasetDirectoryPath + File.separator + datasetDirectoryName + ".tif");
                        break;
                    case SHP:
                        validateShp();
                        break;
                }
                break;

            default:
                throw new GisValidationException(fileExtension + " is not a supported file extension!");
        }
    }

    private void validateAsc(String filename) throws IOException, GisValidationException {
        if (!uploadType.equals(UploadType.ASC)) {
            throw new GisValidationException("The file extension does not match the upload type!");
        }

        ArcGridReader reader;
        try {
            Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:27200"));
            reader = new ArcGridReader(new File(filename), hints);
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new GisValidationException(e.getMessage());
        }
        GridCoverage2D coverage = reader.read(null);

        coordinateReferenceSystem = coverage.getCoordinateReferenceSystem2D();

        GeoTiffWriter writer = new GeoTiffWriter(new File(uploadDir + File.separator + FilenameUtils.getBaseName(filename) + ".tif"));
        writer.write(coverage, null);
        writer.dispose();

        datasetName = datasetDirectoryName;
    }

    private void validateGeoTiff(String filename) throws GisValidationException, IOException {
        if (!uploadType.equals(UploadType.GEOTIFF)) {
            throw new GisValidationException("The file extension does not match the upload type!");
        }
        coordinateReferenceSystem = initRasterFile(new GeoTiffReader(filename));
        datasetName = datasetDirectoryName;
    }

    private void validateShp() throws GisImportException, IOException {
        datasetName = findShpDatasetName();

        if (zipHasDirectory) {
            createZipwithoutDirectory();
        }

        File file = new File(datasetDirectoryPath + File.separator + datasetName + ".shp");
        coordinateReferenceSystem = initShpFile(file);
    }

    private UploadType determinUploadType(String filename) throws IOException, GisValidationException {

        String extension = FilenameUtils.getExtension(filename);
        switch (extension) {
            case "asc":
                return UploadType.ASC;

            case "tif":
                return UploadType.GEOTIFF;

            case "zip":
                ZipFile zipFile = new ZipFile(filename);
                Enumeration zipEntries = zipFile.entries();

                while (zipEntries.hasMoreElements()) {
                    ZipEntry zip = (ZipEntry) zipEntries.nextElement();

                    switch (FilenameUtils.getExtension(zip.getName())) {
                        case "asc":
                            return UploadType.ASC;
                        case "tif":
                            return UploadType.GEOTIFF;
                        case "shp":
                            return UploadType.SHP;
                    }
                }
                break;
        }

        throw new GisValidationException("could not detect your upload type!");
    }


    // GeoServer can not handle directories
    private boolean zipHasDirectory(String filename) throws IOException {
        ZipFile zipFile = new ZipFile(filename);
        Enumeration zipEntries = zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry zip = (ZipEntry) zipEntries.nextElement();
            if (zip.isDirectory() && !zip.getName().equals("__MACOSX/")) {
                return true;
            }
        }
        return false;
    }

    private String unzip(String filename) throws IOException {
        String unzipDirectory = uploadDir;

        UnzipUtility unzipper = new UnzipUtility();
        unzipper.unzip(filename, unzipDirectory);

        return unzipDirectory;
    }

    private void createZipwithoutDirectory() {
        ZipWriter zw = new ZipWriter();
        String path = datasetDirectoryPath + File.separator + datasetDirectoryName;
        zw.createZip(path, path + ".zip");
    }

    private String findShpDatasetName() throws GisImportException {
        File folder = new File(datasetDirectoryPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("shp")) {
                    return FilenameUtils.getBaseName(f.getName());
                }
            }
        }
        throw new GisImportException("Shp dataset name could not be detected!");
    }

    private CoordinateReferenceSystem initShpFile(File file) throws IOException {
        Map<String, Object> map = new HashMap<>();

        map.put("url", file.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);

        datasetName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(datasetName);

        return source.getInfo().getCRS();
    }

    private <T extends GridCoverage2DReader> CoordinateReferenceSystem initRasterFile(T reader) throws IOException {
        GridCoverage2D coverage = reader.read(null);

        return coverage.getCoordinateReferenceSystem2D();
    }

    String getDatasetName() {
        return datasetName;
    }

    CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    UploadType getUploadType() {
        return uploadType;
    }
}

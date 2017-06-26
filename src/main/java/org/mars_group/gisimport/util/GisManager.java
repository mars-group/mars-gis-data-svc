package org.mars_group.gisimport.util;

import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.mars_group.gisimport.exceptions.GisImportException;
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class GisManager {
    private boolean zipHasDirectory;
    private String uploadDir;
    private String zipDirectoryName;
    private List<String> unsupportedFiles = new ArrayList<>();
    private GisType gisType;
    private String dataName;
    private double[] topRightBound;
    private double[] bottomLeftBound;
    private CoordinateReferenceSystem coordinateReferenceSystem;


    /**
     * Validates your GIS file
     *
     * @param uploadDir the upload directory
     * @param filename  this has to be either .zip .tif or .asc
     */
    public GisManager(String uploadDir, String filename) throws IOException, GisValidationException, GisImportException {
        this.uploadDir = uploadDir;
        topRightBound = new double[2];
        bottomLeftBound = new double[2];

        String fileExtension = FilenameUtils.getExtension(filename);

        switch (fileExtension.toLowerCase()) {
            case "asc":
                dataName = FilenameUtils.getBaseName(filename);
                gisType = GisType.ASC;
                coordinateReferenceSystem = initAscFile(filename);
                break;

            case "tif":
                dataName = FilenameUtils.getBaseName(filename);
                gisType = GisType.TIF;
                coordinateReferenceSystem = initGeoTiffFile(filename);
                break;

            case "zip":
                determineDataTypeAndCheckForDirectory(filename);

                unzip(filename);

                if (unsupportedFiles.size() > 0) {
                    removeUnsupportedFiles();
                }

                if (zipHasDirectory && gisType == GisType.SHP) {
                    createZipWithoutDirectory(this.uploadDir + File.separator + dataName);
                }

                String fileBasename = this.uploadDir + File.separator + dataName;
                switch (gisType) {
                    case ASC:
                        coordinateReferenceSystem = initAscFile(fileBasename + ".asc");
                        break;
                    case TIF:
                        coordinateReferenceSystem = initGeoTiffFile(fileBasename + ".tif");
                        break;
                    case SHP:
                        coordinateReferenceSystem = initShapeFile(fileBasename);
                        break;
                }
                break;

            default:
                throw new GisValidationException(fileExtension + " is not a supported file extension!");
        }
    }

    // this method does 2 things, but saves a whole iteration over the zip content
    private void determineDataTypeAndCheckForDirectory(String filename) throws IOException, GisValidationException {
        ZipFile zipFile = new ZipFile(filename);
        Enumeration zipEntries = zipFile.entries();

        /*
         * We want to know if the files are inside a directory and detect the extension type.
         * Since we walk though the zip from the root, we can stop looking for directories,
         * once we found a valid extension.
         */
        while (zipEntries.hasMoreElements()) {
            ZipEntry zip = (ZipEntry) zipEntries.nextElement();

            if (zip.getName().contains("__MACOSX")) {
                unsupportedFiles.add(zip.getName());
                continue;
            }

            if (zip.isDirectory()) {
                zipHasDirectory = true;
                zipDirectoryName = zip.getName();
                continue;
            }

            // determine data type and name
            switch (FilenameUtils.getExtension(zip.getName()).toLowerCase()) {
                case "asc":
                    gisType = GisType.ASC;
                    dataName = FilenameUtils.getBaseName(zip.getName());
                    break;
                case "tif":
                    gisType = GisType.TIF;
                    dataName = FilenameUtils.getBaseName(zip.getName());
                    break;
                case "shp":
                    gisType = GisType.SHP;
                    dataName = zip.getName();
                    break;
                // all files a ShapeFile can contain. Everything else is not allowed
                case "shx":
                case "dbf":
                case "prj":
                case "sbn":
                case "sbx":
                case "fbn":
                case "fbx":
                case "ain":
                case "aih":
                case "ixs":
                case "mxs":
                case "atx":
                case "xml":
                case "cpg":
                case "qix":
                    break;
                default:
                    unsupportedFiles.add(zip.getName());
            }
        }

        if (gisType == null) {
            throw new GisValidationException("could not detect your upload type inside Zip file!");
        }
    }

    private void unzip(String filename) throws IOException {
        UnzipUtility unzipper = new UnzipUtility();
        unzipper.unzip(filename, uploadDir);
    }

    private void removeUnsupportedFiles() {
        for (String filename : unsupportedFiles) {
            new File(uploadDir + File.separator + filename).delete();
        }
    }

    private void createZipWithoutDirectory(String filename) {
        String directory = uploadDir + File.separator + zipDirectoryName;
        String outputFile = uploadDir + File.separator + FilenameUtils.getBaseName(filename) + ".zip";

        new ZipWriter().createZip(directory, outputFile);
    }

    private CoordinateReferenceSystem initAscFile(String filename) throws IOException, GisValidationException {
        ArcGridReader reader;
        try {
            Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:27200"));
            reader = new ArcGridReader(new File(filename), hints);
        } catch (FactoryException e) {
            e.printStackTrace();
            throw new GisValidationException(e.getMessage());
        }
        GridCoverage2D coverage = reader.read(null);

        GeoTiffWriter writer = new GeoTiffWriter(new File(uploadDir + File.separator + FilenameUtils.getBaseName(filename) + ".tif"));
        writer.write(coverage, null);
        writer.dispose();

        setBounds(coverage.getGridGeometry().getEnvelope2D().getBounds());

        return coverage.getCoordinateReferenceSystem2D();

    }

    private CoordinateReferenceSystem initGeoTiffFile(String filename) throws IOException {
        GridCoverage2D coverage = new GeoTiffReader(filename).read(null);

        setBounds(coverage.getGridGeometry().getEnvelope2D().getBounds());

        return coverage.getCoordinateReferenceSystem2D();
    }

    private CoordinateReferenceSystem initShapeFile(String filename) throws IOException {
        Map map = Collections.singletonMap("url", new File(filename).toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);

        dataName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(dataName);

        setBounds(source.getBounds());

        return source.getInfo().getCRS();
    }

    private void setBounds(ReferencedEnvelope bounds) {
        topRightBound = bounds.getUpperCorner().getCoordinate();
        bottomLeftBound = bounds.getLowerCorner().getCoordinate();
    }

    private void setBounds(Rectangle bounds) {
        topRightBound[0] = bounds.getMaxX();
        topRightBound[1] = bounds.getMaxY();
        bottomLeftBound[0] = bounds.getMinX();
        bottomLeftBound[1] = bounds.getMinY();
    }

    public String getDataName() {
        return dataName;
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

    public GisType getGisType() {
        return gisType;
    }

    public double[] getTopRightBound() {
        return topRightBound;
    }

    public double[] getBottomLeftBound() {
        return bottomLeftBound;
    }
}

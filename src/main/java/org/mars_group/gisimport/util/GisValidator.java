package org.mars_group.gisimport.util;

import org.apache.commons.io.FileUtils;
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
import org.mars_group.gisimport.exceptions.GisValidationException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class GisValidator {
    private GisType gisType;
    private String shpBasename;
    private String filename;
    private String timeseriesFilename;
    private double[] topRightBound;
    private double[] bottomLeftBound;
    private CoordinateReferenceSystem coordinateReferenceSystem;

    /**
     * Validates your GIS file
     *
     * @param filename Path to the file. This has to be either .zip .tif or .asc
     */
    public GisValidator(String filename) throws IOException, FactoryException, GisValidationException {
        this.filename = filename;

        topRightBound = new double[2];
        bottomLeftBound = new double[2];

        if (FilenameUtils.getExtension(this.filename).toLowerCase().equals("zip")) {
            String unzipDir = FilenameUtils.getPath(this.filename);
            ZipUtility.unzip(this.filename, unzipDir);

            setFilenameFromZipDirAndDeleteOtherFiles(unzipDir);
        }

        switch (FilenameUtils.getExtension(this.filename).toLowerCase()) {
            case "asc":
                coordinateReferenceSystem = ConvertToGeoTiffAndGetCrs();
                gisType = GisType.ASC;
                break;

            case "tif":
                gisType = GisType.TIF;
                coordinateReferenceSystem = getCrsFromGeoTiff();
                break;

            case "shp":
                gisType = GisType.SHP;
                coordinateReferenceSystem = getCrsFromShape();

                String dirName = FilenameUtils.getPath(this.filename);
                shpBasename = FilenameUtils.getBaseName(this.filename);
                this.filename = dirName + shpBasename + ".zip";

                ZipUtility.createZip(dirName, this.filename);
                break;

            default:
                throw new GisValidationException(FilenameUtils.getExtension(this.filename) + " is not a supported file extension!");
        }
    }

    private void setFilenameFromZipDirAndDeleteOtherFiles(String zipDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(zipDirectory))) {
            paths.forEach(path -> {
                switch (FilenameUtils.getExtension(path.toString()).toLowerCase()) {
                    case "asc":
                    case "tif":
                    case "shp":
                        filename = path.toString();
                        break;
                    // Timeseries file
                    case "csv":
                        if (timeseriesFilename != null) {
                            System.out.println("Warning, multiple timeseries files detected! The first one was chosen.");
                        } else {
                            timeseriesFilename = path.toString();
                        }
                        break;
                    // all files a ShapeFile can contain.
                    case "ain":
                    case "aih":
                    case "atx":
                    case "cpg":
                    case "dbf":
                    case "fbn":
                    case "fbx":
                    case "ixs":
                    case "mxs":
                    case "prj":
                    case "qix":
                    case "qpj":
                    case "sbn":
                    case "sbx":
                    case "shx":
                    case "xml":
                        break;
                    default:
                        deleteFile(path.toString());
                }
            });
        }
    }

    private CoordinateReferenceSystem ConvertToGeoTiffAndGetCrs() throws FactoryException, IOException, GisValidationException {
        ArcGridReader reader;
        Hints hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:4326"));
        reader = new ArcGridReader(new File(filename), hints);

        GridCoverage2D coverage;
        try {
            coverage = reader.read(null);
        } catch (IllegalArgumentException e) {
            throw new GisValidationException("Failed to parse input file, make sure it is valid!");
        }

        filename = FilenameUtils.getPath(filename) + File.separator + FilenameUtils.getBaseName(filename) + ".tif";

        GeoTiffWriter writer = new GeoTiffWriter(new File(filename));
        writer.write(coverage, null);
        writer.dispose();

        setBounds(coverage.getGridGeometry().getEnvelope2D().getBounds());

        return handleInvalidCrs(coverage.getCoordinateReferenceSystem2D());
    }

    private CoordinateReferenceSystem getCrsFromGeoTiff() throws IOException, FactoryException {
        GridCoverage2D coverage = new GeoTiffReader(filename).read(null);

        setBounds(coverage.getGridGeometry().getEnvelope2D().getBounds());

        return handleInvalidCrs(coverage.getCoordinateReferenceSystem2D());
    }

    private CoordinateReferenceSystem getCrsFromShape() throws IOException, FactoryException {
        Map<String, Object> map = new HashMap<>();

        map.put("url", new File(filename).toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);

        shpBasename = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(shpBasename);

        setBounds(source.getBounds());

        return handleInvalidCrs(source.getInfo().getCRS());
    }

    private CoordinateReferenceSystem handleInvalidCrs(CoordinateReferenceSystem crs) throws FactoryException {
        if (crs == null) {
            crs = CRS.decode("EPSG:4326");
        }

        return crs;
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

    private void deleteFile(String filename) {
        try {
            System.out.println("Deleting: " + filename);
            FileUtils.forceDelete(new File(filename));
        } catch (IOException e) {
            System.out.println("Failed to delete file: " + filename);
            e.printStackTrace();
        }
    }

    public GisType getGisType() {
        return gisType;
    }

    public String getShpBasename() {
        return shpBasename;
    }

    public String getFilename() {
        return filename;
    }

    public String getTimeseriesFilename() {
        return timeseriesFilename;
    }

    public double[] getTopRightBound() {
        return topRightBound;
    }

    public double[] getBottomLeftBound() {
        return bottomLeftBound;
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }

}
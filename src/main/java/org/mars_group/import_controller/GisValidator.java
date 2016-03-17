package org.mars_group.import_controller;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GisValidator {
    private File file;

    public GisValidator() {
        file = new File("upload-dir/TM_WORLD_BORDERS.shp");
        System.out.println(getCoordinateReferenceSystem());
    }

    public GisValidator(File file) {
        this.file = file;
    }

    // GeoServer can not handle directories
    public boolean zipHasDirectory() {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
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

    public String getCoordinateReferenceSystem() {
        System.out.println("getCoordinateReferenceSystem()");

        Map<String, Object> map = new HashMap<>();


        try {
            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    System.out.print(feature.getID());
                    System.out.print(": ");
                    System.out.println(feature.getDefaultGeometryProperty().getValue());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "EPSG:4326";
    }

}

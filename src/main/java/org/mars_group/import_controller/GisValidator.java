package org.mars_group.import_controller;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GisValidator {


    // GeoServer can not handle directories
    public boolean zipHasDirectory(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration zipEntries = zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry zip = (ZipEntry) zipEntries.nextElement();
            if (zip.isDirectory()) {
                return true;
            }
        }
        return false;
    }

}

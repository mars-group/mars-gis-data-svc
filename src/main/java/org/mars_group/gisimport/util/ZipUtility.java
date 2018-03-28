package org.mars_group.gisimport.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ZipUtility {
    private static final byte[] buffer = new byte[4096];

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     */
    public static void unzip(String filename, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            Assert.isTrue(destDir.mkdir(), "Creating directory failed: " + destDir);
        }

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(filename));
        ZipEntry zipEntry;

        // iterates over entries in the zip file
        while ((zipEntry = zipIn.getNextEntry()) != null) {
            if (zipEntry.getName().contains("__MACOSX")) {
                continue;
            }

            if (!zipEntry.isDirectory()) {
                String filePath = destDirectory + FilenameUtils.getName(zipEntry.getName());
                extractFile(zipIn, filePath);
            }
        }
        zipIn.closeEntry();
        zipIn.close();

        try {
            FileUtils.forceDelete(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        int read;
        while ((read = zipIn.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        bos.close();
    }

}
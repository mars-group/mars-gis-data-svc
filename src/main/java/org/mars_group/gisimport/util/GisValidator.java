package org.mars_group.gisimport.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mars_group.gisimport.exceptions.GisValidationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class GisValidator {
    private GisType originalGisType;
    private GisType gisType;
    private String filename;
    private String title;
    private String timeseriesFilename;

    /**
     * Validates your GIS file
     *
     * @param filename Path to the file. This has to be either .zip .tif or .asc
     */
    public GisValidator(String filename, String title) throws IOException, GisValidationException {
        this.filename = filename;
        this.title = title;

        if (FilenameUtils.getExtension(this.filename).toLowerCase().equals("zip")) {
            String unzipDir = FilenameUtils.getPath(this.filename);
            ZipUtility.unzip(this.filename, unzipDir);

            setFilenameFromZipDirAndDeleteOtherFiles(unzipDir);
        }

        switch (FilenameUtils.getExtension(this.filename).toLowerCase()) {
            case "asc":
                originalGisType = GisType.ASC;
                gisType = originalGisType;
                break;

            case "tif":
                originalGisType = GisType.TIF;
                gisType = originalGisType;
                convertToAsciiGrid();
                break;

            case "json":
            case "geojson":
                originalGisType = GisType.GJSON;
                gisType = originalGisType;
                break;

            case "shp":
                originalGisType = GisType.SHP;
                gisType = originalGisType;
                convertToGeoJson();
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

    private void deleteFile(String filename) {
        try {
            System.out.println("Deleting: " + filename);
            FileUtils.forceDelete(new File(filename));
        } catch (IOException e) {
            System.out.println("Failed to delete file: " + filename);
            e.printStackTrace();
        }
    }

    private void convertToAsciiGrid() throws IOException, GisValidationException {
        if (!gisType.equals(GisType.TIF)) {
            return;
        }

        System.out.println(title + ": Converting file to AsciiGrid ...");

        String newFilename = FilenameUtils.getPath(filename) + FilenameUtils.getBaseName(filename) + ".asc";

        convertFile(new String[]{
                "gdal_translate", "-of", "AAIGrid", "-co", "force_cellsize=true", "-b", "1", filename, newFilename
        });

        filename = newFilename;
        gisType = GisType.ASC;
    }

    private void convertToGeoJson() throws IOException, GisValidationException {
        if (!gisType.equals(GisType.SHP)) {
            return;
        }

        System.out.println(title + ": Converting file to GeoJSON ...");

        String newFilename = FilenameUtils.getPath(filename) + FilenameUtils.getBaseName(filename) + ".json";

        convertFile(new String[]{"ogr2ogr", "-f", "GeoJSON", newFilename, filename});

        filename = newFilename;
        gisType = GisType.GJSON;
    }

    private void convertFile(String[] command) throws IOException, GisValidationException {
        Process process = Runtime.getRuntime().exec(command);

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new GisValidationException("File conversion failed. Error: " + e.getMessage());
        }

        if (process.exitValue() != 0) {
            try (InputStream inputStream = process.getErrorStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String s;
                StringBuilder message = new StringBuilder();
                while ((s = reader.readLine()) != null) {
                    message.append(s);
                }

                throw new GisValidationException("Error converting file: " + message.toString());
            }
        }

        if (!new File(command[command.length - 1]).exists()) {
            throw new GisValidationException("The converted file does not exist!");
        }
    }

    public GisType getOriginalGisType() {
        return originalGisType;
    }

    public GisType getGisType() {
        return gisType;
    }

    public String getFilename() {
        return filename;
    }

    public String getTimeseriesFilename() {
        return timeseriesFilename;
    }

}

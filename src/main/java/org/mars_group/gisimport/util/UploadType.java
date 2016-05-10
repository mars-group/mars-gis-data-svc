package org.mars_group.gisimport.util;

public enum UploadType {
    ASC("AsciiGrid"), TIF("Geotiff"), SHP("Shapefile");

    private String name;

    UploadType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

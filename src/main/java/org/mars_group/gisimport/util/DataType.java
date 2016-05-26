package org.mars_group.gisimport.util;

public enum DataType {
    ASC("AsciiGrid"), TIF("Geotiff"), SHP("Shapefile");

    private String name;

    DataType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

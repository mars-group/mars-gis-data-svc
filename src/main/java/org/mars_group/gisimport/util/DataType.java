package org.mars_group.gisimport.util;

public enum DataType {
    ASC("asciigrid"), TIF("geotiff"), SHP("shapefile");

    private String name;

    DataType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

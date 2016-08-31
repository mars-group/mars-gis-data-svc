package org.mars_group.gisimport.util;

public enum DataType {
    ASC("ASCIIGRID"), TIF("GEOTIFF"), SHP("SHAPEFILE");

    private String name;

    DataType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

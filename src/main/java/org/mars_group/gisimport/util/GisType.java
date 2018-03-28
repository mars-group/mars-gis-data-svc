package org.mars_group.gisimport.util;

public enum GisType {
    ASC("ASCIIGRID"), TIF("GEOTIFF"), GJSON("GEOJSON"), SHP("SHAPEFILE");

    private final String name;

    GisType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

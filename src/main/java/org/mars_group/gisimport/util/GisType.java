package org.mars_group.gisimport.util;

public enum GisType {
    ASC("ASCIIGRID"), GJSON("GEOJSON"), TIF("GEOTIFF"), SHP("SHAPEFILE");

    private String name;

    GisType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

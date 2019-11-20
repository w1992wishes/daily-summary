package me.w1992wishes.hbase.inaction.geo;

/**
 * @author w1992wishes 2019/11/20 9:59
 */
public class QueryMatch {

    public String id;
    public String hash;
    public double lon, lat;
    public double distance = Double.NaN;

    public QueryMatch(String id, String hash, double lon, double lat) {
        this.id = id;
        this.hash = hash;
        this.lon = lon;
        this.lat = lat;
    }

}

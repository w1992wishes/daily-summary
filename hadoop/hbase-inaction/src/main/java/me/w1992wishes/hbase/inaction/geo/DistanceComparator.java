package me.w1992wishes.hbase.inaction.geo;

import java.awt.geom.Point2D;
import java.util.Comparator;

/**
 * @author w1992wishes 2019/11/20 10:02
 */
public class DistanceComparator implements Comparator<QueryMatch> {

    Point2D origin;

    public DistanceComparator(double lon, double lat) {
        this.origin = new Point2D.Double(lon, lat);
    }

    @Override
    public int compare(QueryMatch o1, QueryMatch o2) {
        if (Double.isNaN(o1.distance)) {
            o1.distance = origin.distance(o1.lon, o1.lat);
        }
        if (Double.isNaN(o2.distance)) {
            o2.distance = origin.distance(o2.lon, o2.lat);
        }
        return Double.compare(o1.distance, o2.distance);
    }
}

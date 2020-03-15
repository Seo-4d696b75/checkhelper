package jp.ac.u_tokyo.t.seo.station.diagram;

/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public class Circle {

    public Circle(Point center, double radius){
        this.center = center;
        this.radius = radius;
    }

    public final Point center;
    public final double radius;

    public boolean containsPoint(Point point){
        return Point.measure(point, center) < radius;
    }

}

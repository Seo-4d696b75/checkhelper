package jp.ac.u_tokyo.t.seo.station.diagram;


/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public class BasePoint extends Point{

    public BasePoint(double x, double y){
        this.x = x;
        this.y = y;
    }

    private final double x,y;

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

}

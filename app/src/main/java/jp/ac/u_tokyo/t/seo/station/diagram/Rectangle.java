package jp.ac.u_tokyo.t.seo.station.diagram;

import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public class Rectangle {

    public Rectangle(double left, double top, double right, double bottom){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public final double left,top,right,bottom;

    public Triangle getContainer(){
        double x = (left + right) / 2;
        double y = (top + bottom) / 2;
        double r = Math.sqrt(Math.pow(left-right,2) + Math.pow(top-bottom,2));
        Point a = new ContainerPoint(x-Math.sqrt(3)*r, y+r, "A");
        Point b = new ContainerPoint(x+Math.sqrt(3)*r, y+r, "B");
        Point c = new ContainerPoint(x, y-2*r, "C");
        return new Triangle(a,b,c);
    }

    public double getWidth(){
        return right - left;
    }

    public double getHeight(){
        return top - bottom;
    }

    public Point getCenter(){
        return new BasePoint((right + left)/2, (top + bottom)/2);
    }

    private static class ContainerPoint extends BasePoint{

        ContainerPoint(double x, double y, String symbol){
            super(x, y);
            this.symbol = symbol;
        }

        private final String symbol;

        @Override
        public String toString(){
            return String.format(Locale.US, "Container{%s,pos:(%.4f,%.4f)}", symbol, getX(), getY());
        }

    }
}

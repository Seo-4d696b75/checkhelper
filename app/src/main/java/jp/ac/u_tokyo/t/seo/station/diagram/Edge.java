package jp.ac.u_tokyo.t.seo.station.diagram;

import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public class Edge {

    public Edge(Point a, Point b){
        //ｘ(またはｙ)座標に関してa<=bを満たすように整理
        //ｘ座標が一致した場合はｙ座標に関して昇順
        if ( a.getX() < b.getX() ) {
            this.a = a;
            this.b = b;
        }else if ( a.getX() > b.getX()){
            this.a = b;
            this.b = a;
        }else{
            if ( a.getY() < b.getY() ){
                this.a = a;
                this.b = b;
            }else if ( a.getY() > b.getY() ) {
                this.a = b;
                this.b = a;
            }else{
                throw new IllegalArgumentException("point duplicated : " + a.toString());
            }
        }
    }

    public final Point a,b;

    private Line line;

    @Override
    public boolean equals(Object other){
        if ( other instanceof Edge ){
            Edge edge = (Edge)other;
            return a.equals(edge.a) && b.equals(edge.b);
        }
        return false;
    }

    @Override
    public int hashCode(){
        int result = 17;
        result = result * 31 + a.hashCode();
        result = result * 31 + b.hashCode();
        return result;
    }

    @Override
    public String toString(){
        return String.format(Locale.US, "Edge{%s-%s}", a.toString(), b.toString());
    }

    public Point getMiddlePoint(){
        return new BasePoint((a.getX() + b.getX())/2, (a.getY() + b.getY())/2);
    }

    public static boolean onEdge(Point start, Point end, Point p){
        return (start.getX() - p.getX()) * (end.getX() - p.getX()) + (start.getY() - p.getY()) * (end.getY() - p.getY()) <= 0 && Line.onLine(start, end, p);
    }

    /**
     * この線分を(index):(1-index)の比で内分する点を計算します.<br>
     * <strong>注意 </strong>{@link Point#compareTo(Point) 座標点の自然順序付け}によって決まるa<bの関係において、
     * 線分ABを(index):(1-index)の比で内分するので方向に注意する.{@code index<0}の場合は外分点になる.
     * @param index (0,1) => 内分点
     * @return Non Null
     */
    public Point getDivision(double index){
        return Point.getDivision(a, b, index);
    }

    public boolean onEdge(Point p){
        return onEdge(this.a, this.b, p);
    }

    /**
     * Equals to call {@link #toLine()} and {@link Line#onSameSide(Point, Point)}
     * @param p1
     * @param p2
     * @return
     */
    public boolean onSameSide(Point p1, Point p2){
        return toLine().onSameSide(p1, p2);
    }

    public Line toLine(){
        if ( line == null ){
            line = new Line(this);
        }
        return line;
    }

    /**
     * 線分との交点を取得する.
     * <strong>注意 </strong>交点を持たない場合はNull
     * @param line 交点を調べる直線
     * @return Null if no such intersection
     */
    public Point getIntersection(Line line){
        Point p = this.toLine().getIntersection(line);
        double v = (a.getX() - p.getX()) * (b.getX() - p.getX()) + (a.getY() - p.getY()) * (b.getY() - p.getY());
        if ( v <= 0 ){
            return p;
        }else{
            return null;
        }
    }

    public Point getIntersection(Edge edge){
        Point p = edge.getIntersection(this.toLine());
        if ( p != null ){
            double v = (a.getX() - p.getX()) * (b.getX() - p.getX()) + (a.getY() - p.getY()) * (b.getY() - p.getY());
            if ( v <= 0 ){
                return p;
            }
        }
        return null;
    }

    public double getDistance(Point p){
        double v1 = (p.getX() - a.getX()) * (b.getX() - a.getX()) + (p.getY() - a.getY()) * (b.getY() - a.getY());
        double v2 = (p.getX() - b.getX()) * (a.getX() - b.getX()) + (p.getY() - b.getY()) * (a.getY() - b.getY());
        if ( v1 > 0 && v2 > 0 ){
            return toLine().getDistance(p);
        }else if ( v1 < 0 ){
            return p.measure(a);
        }else{
            return p.measure(b);
        }
    }

}

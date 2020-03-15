package jp.ac.u_tokyo.t.seo.station.diagram;


import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public class Triangle {




    /**
     * 三点を結ぶ三角形が存在するなら三角形オブジェクトを取得する
     * @return 一致する2点を含む、または3点が一直線上に存在する場合はnull
     */
    public static Triangle getTriangle(Point a, Point b, Point c){
        if ( Line.onLine(a,b,c) ){
            return null;
        }else{
            return new Triangle(a,b,c);
        }
    }

    public static Triangle getTriangle(Edge edge, Point p){
        if ( Line.onLine(edge.a, edge.b, p)){
            return null;
        }else{
            return new Triangle(edge, p);
        }
    }

    public Triangle(Point a, Point b, Point c){
        if ( Line.onLine(a, b, c) ){
            throw new IllegalArgumentException("3 points on a line.");
        }
        //x座標に関してa<=b<=cを満たす順番に整理する
        //ｘ座標が一致した場合はｙ座標に関して昇順
        if ( a.getX() > b.getX() ){
            Point temp = a;
            a = b;
            b = temp;
        }
        if ( b.getX() > c.getX() ){
            Point temp = b;
            b = c;
            c = temp;
            if ( a.getX() > b.getX() ){
                temp = a;
                a = b;
                b = temp;
            }
        }
        if ( a.getX() == b.getX() && a.getY() > b.getY() ){
            Point temp = a;
            a = b;
            b = temp;
        }
        if ( b.getX() == c.getX() && b.getY() > c.getY() ){
            Point temp = b;
            b = c;
            c = temp;
            if ( a.getX() == b.getX() && a.getY() > b.getY()){
                temp = a;
                a = b;
                b = temp;
            }
        }
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public  Triangle(Edge edge, Point p){
        this(edge.a, edge.b, p);
    }

    public final Point a,b,c;

    private Circle circumscribed;
    private Edge ab, bc, ca;

    @Override
    public String toString(){
        return String.format(Locale.US, "Triangle{%s-%s-%s}", a.toString(), b.toString(), c.toString());
    }

    @Override
    public boolean equals(Object other){
        if ( other instanceof Triangle ){
            Triangle t = (Triangle)other;
            return a.equals(t.a) && b.equals(t.b) && c.equals(t.c);
        }
        return false;
    }

    @Override
    public int hashCode(){
        int result = 17;
        result = result * 31 + a.hashCode();
        result = result * 31 + b.hashCode();
        result = result * 31 + c.hashCode();
        return result;
    }

    private void setEdge(){
        if ( ab == null || bc == null || ca == null ){
            ab = new Edge(a, b);
            bc = new Edge(b, c);
            ca = new Edge(c, a);
        }
    }

    public Point[] getIntersection(Line line){
        setEdge();
        Point a = bc.getIntersection(line);
        Point b = ca.getIntersection(line);
        Point c = ab.getIntersection(line);
        int cnt = 0;
        if ( a != null ) cnt++;
        if ( b != null ) cnt++;
        if ( c != null ) cnt++;
        Point[] results = new Point[cnt];
        int index = 0;
        if ( a != null ) results[index++] = a;
        if ( b != null ) results[index++] = b;
        if ( c != null ) results[index] = c;
        if ( cnt == 2 && results[0].compareTo(results[1]) > 0 ){
            Point temp = results[0];
            results[0] = results[1];
            results[1] = temp;
        }
        return results;
    }

    Point[] getIntersection(Edge edge){
        setEdge();
        Point a = edge.getIntersection(bc.toLine());
        Point b = edge.getIntersection(ca.toLine());
        Point c = edge.getIntersection(ab.toLine());
        int cnt = 0;
        if ( a != null ) cnt++;
        if ( b != null ) cnt++;
        if ( c != null ) cnt++;
        Point[] results = new Point[cnt];
        int index = 0;
        if ( a != null ) results[index++] = a;
        if ( b != null ) results[index++] = b;
        if ( c != null ) results[index] = c;
        return results;
    }

    Edge getOppositeSize(Point p){
        if ( Point.isMatch(a, p) ){
            return new Edge(b, c);
        }else if ( Point.isMatch(b, p) ){
            return new Edge(c, a);
        }else if ( Point.isMatch(c, p) ){
            return new Edge(a, b);
        }
        return null;
    }

    /**
     * 指定された点がこの三角形に含まれるか判定します
     * <strong>NOTE </strong>三角形の辺上および頂点上も含みます
     * また計算過程の0判定に{@link Setting#error}の値が用いられます
     * @return 三角形の内部・辺上・頂点上に含まれる場合はtrue
     */
    public boolean containsPoint(Point point){
        double x1 = a.getX() - point.getX();
        double y1 = a.getY() - point.getY();
        double x2 = b.getX() - point.getX();
        double y2 = b.getY() - point.getY();
        double x3 = c.getX() - point.getX();
        double y3 = c.getY() - point.getY();
        double v1 = x1*y2 - y1*x2;
        double v2 = x2*y3 - y2*x3;
        double v3 = x3*y1 - y3*x1;
        return ( Setting.greaterThanZero(v1) && Setting.greaterThanZero(v2) && Setting.greaterThanZero(v3) ) ||
                ( Setting.lessThanZero(v1) && Setting.lessThanZero(v2) && Setting.lessThanZero(v3) );
    }

    public boolean isVertex(Point point){
        return Point.isMatch(point, a) || Point.isMatch(point, b) || Point.isMatch(point, c);
    }

    public boolean isEdge(Edge edge){
        return isVertex(edge.a) && isVertex(edge.b);
    }

    public boolean hasSameVertex(Triangle other){
        return isVertex(other.a) || isVertex(other.b) || isVertex(other.c);
    }

    public Circle getCircumscribed(){
        if ( circumscribed == null ){
            double cc = 2*((b.getX()-a.getX())*(c.getY()-a.getY()) - (b.getY()-a.getY())*(c.getX()-a.getX()));
            if ( Setting.isZero(cc) ){
                throw new IllegalArgumentException("error value too small");
            }
            //http://tercel-sakuragaoka.blogspot.jp/2011/06/processingdelaunay_3958.html
            double p = b.getX()*b.getX() - a.getX()*a.getX() + b.getY()*b.getY() - a.getY()*a.getY();
            double q = c.getX()*c.getX() - a.getX()*a.getX() + c.getY()*c.getY() - a.getY()*a.getY();
            Point center = new BasePoint(
                    ((c.getY()-a.getY())*p + (a.getY()-b.getY())*q) / cc,
                    ((a.getX()-c.getX())*p + (b.getX()-a.getX())*q) / cc
            );
            double r = Point.measure(center, a);
            circumscribed = new Circle(center, r);
        }
        return circumscribed;
    }


}

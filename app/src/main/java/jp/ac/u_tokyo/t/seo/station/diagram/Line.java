package jp.ac.u_tokyo.t.seo.station.diagram;

/**
 * ユークリッド平面における直線を表現します.
 * 一般に直線は適当な3パラメータを用いてax+by+c=0の形で表現できます.ただしa==0 && b==0 は除きます
 * 初期化時にこの3パラメータは以下のように正規化されますので誤差に注意が必要です.<br>
 * <Ul>
 *     <li>b!=0の時はb=1となるように</li>
 *     <li>b==0の時はa=1となるように</li>
 * </ul>
 * @author Seo-4d696b75
 * @version 2018/05/17
 */
public class Line {

    /**
     * 三点が一直線上に存在するか判定する
     * ひとつの点から2点へ延びるベクトルの外積を0判定します
     * <strong>NOTE</strong> 0判定には{@link Setting#error}の値が用いられます
     * @return 一直線上に存在する、または一致する2点を含む場合はtrue
     */
    public static boolean onLine(Point a, Point b, Point c){
        //外積を計算
        double v = (b.getX()-a.getX())*(c.getY()-a.getY()) - (b.getY()-a.getY())*(c.getX()-a.getX());
        return Setting.isZero(v);
    }

    /**
     * 指定した傾きと切片をもつ直線を取得します
     * @param gradient 傾き=ｘ座標方向に単位距離動いたときのｙ座標方向の変化量
     * @param intercept y切片=ｘ座標が0の時のｙ座標の値
     */
    public Line(double gradient, double intercept){
        a = -gradient;
        b = 1.0;
        c = -intercept;
    }

    public Line(double a, double b, double c){
        //!!! a==0 && b==0 is not allowed !!!
        if ( Setting.isZero(b) ){
            if ( Setting.isZero(a) ) {
                throw new IllegalArgumentException("Not Line in case of a=0, b=0 : ax+by+c=0");
            }
            this.a = 1.0;
            this.b = 0.0;
            this.c = c/a;
        }else {
            this.a = a / b;
            this.b = 1.0;
            this.c = c / b;
        }
    }

    public Line (Point p1, Point p2){
        if ( Point.isMatch(p1, p2) ){
            throw new IllegalArgumentException("same point not define a line");
        }
        if ( Setting.isZero(p1.getX() - p2.getX()) ){
            a = 1.0;
            b = 0.0;
            c = -(p1.getX() + p2.getX())/2;
        }else{
            a = (p2.getY() - p1.getY()) / (p1.getX() - p2.getX());
            b = 1.0;
            c = (p2.getX()*p1.getY() - p1.getX()*p2.getY()) / (p1.getX() - p2.getX());
        }
    }

    public Line (Edge edge){
        this(edge.a, edge.b);
    }

    public final double a;
    public final double b;
    public final double c;

    @Override
    public boolean equals(Object other){
        if ( other instanceof Line ){
            Line line = (Line)other;
            return this.a == line.a && this.b == line.b && this.c == line.c;
        }
        return false;
    }

    @Override
    public int hashCode(){
        int result = 17;
        long a = Double.doubleToLongBits(this.a);
        long b = Double.doubleToLongBits(this.b);
        long c = Double.doubleToLongBits(this.c);
        result = result * 31 + (int)(a ^ (a >> 32));
        result = result * 31 + (int)(b ^ (b >> 32));
        result = result * 31 + (int)(c ^ (c >> 32));
        return result;
    }

    public Point getIntersection(Line line){
        double det = a * line.b - line.a * b;
        if ( Setting.isZero(det) ){
            return null;
        }else{
            return new BasePoint(
                    ( b * line.c - line.b * c )/det,
                    (line.a * c - a * line.c )/det
            );
        }
    }

    public Point getIntersection(Edge edge){
        if ( (a*edge.a.getX() + b*edge.a.getY() + c ) * (a*edge.b.getX() + b*edge.b.getY() + c) <= 0 ){
            return getIntersection(new Line(edge));
        }else{
            return null;
        }
    }

    public static Line getPerpendicularBisector(Edge edge){
        return getPerpendicularBisector(edge.a, edge.b);
    }

    public static Line getPerpendicularBisector(Point p1, Point p2){
        return new Line(
                p1.getX()-p2.getX(),
                p1.getY()-p2.getY(),
                ( -Math.pow(p1.getX(),2) - Math.pow(p1.getY(),2) + Math.pow(p2.getX(),2) + Math.pow(p2.getY(),2)) / 2
        );
    }

    public double getDistance(Point point){
        return Math.abs(point.getX() * a + point.getY() * b + c ) / Math.sqrt(a*a + b*b);
    }

    public boolean onSameSide(Point p1, Point p2){
        double v1 = a * p1.getX() + b * p1.getY() + c;
        double v2 = a * p2.getX() + b * p2.getY() + c;
        return v1 * v2 >= 0;
    }

    public boolean onLine(Point p){
        return Setting.isZero(Math.abs(a * p.getX() + b * p.getY() + c));
    }

}

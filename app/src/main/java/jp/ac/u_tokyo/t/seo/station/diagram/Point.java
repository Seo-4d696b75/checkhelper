package jp.ac.u_tokyo.t.seo.station.diagram;

import java.util.Locale;

/**
 * ユークリッド平面上の点を表す不可変オブジェクトを定義します
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public abstract class Point implements Comparable<Point>{


    /**
     * 点のｘ座標を返します
     * <strong>NOTE</strong> オブジェクトが返す値は不変である必要があります
     */
    public abstract double getX();

    /**
     * 点のｙ座標を返します
     * <strong>NOTE</strong> オブジェクトが返す値は不変である必要があります
     */
    public abstract double getY();

    @Override
    public boolean equals(Object other){
        if ( other instanceof Point){
            Point p = (Point)other;
            return getX() == p.getX() && getY() == p.getY();
        }
        return false;
    }

    /**
     * 許容される誤差の範囲内で点の同値判定を行います
     * @return {@link Setting#error}の範囲内でｘ、ｙ座標の値が一致するならtrue
     */
    public static boolean isMatch(Point p1, Point p2){
        return Setting.isZero(p1.getX() - p2.getX()) && Setting.isZero(p1.getY() - p2.getY());
    }

    @Override
    public int hashCode(){
        //http://typea.info/blg/glob/2014/05/java-hashcode-equals.html
        int result  = 17;
        long fa = Double.doubleToLongBits(getX());
        result = 31 * result + (int)(fa ^ (fa >> 32));
        long fb = Double.doubleToLongBits(getY());
        result = 31 * result + (int)(fb ^ (fb >> 32));
        return result;
    }

    /**
     * この線分ABを(index):(1-index)の比で内分する点を計算します.<br>
     * <strong>注意 </strong>{@code index<0}の場合は外分点になる.
     * @param index (0,1) => 内分点
     * @return Non Null
     */
    public static Point getDivision(Point a, Point b, double index){
        return new BasePoint(
                a.getX() * (1-index) + b.getX() * index,
                a.getY() * (1-index) + b.getY() * index
        );
    }

    public static double measure(Point a, Point b) {
        return Math.sqrt(Math.pow(a.getX()-b.getX(),2)+ Math.pow(a.getY()-b.getY(),2));
    }

    public double measure(Point other){
        return measure(this, other);
    }

    /**
     * この点の簡潔な文字列表現を返します
     */
    @Override
    public String toString(){
        return String.format(Locale.US, "Point(%.4f,%.4f)", getX(), getY());
    }

    public static Point getMiddlePoint(Point p1, Point p2){
        return new BasePoint(p1.getX()/2 + p2.getX()/2, p1.getY()/2 + p2.getY()/2 );
    }

    public static int compare(Point p1, Point p2){
        if ( p1.getX() == p2.getX() ){
            if ( p1.getY() == p2.getY() ){
                return 0;
            }else if(p1.getY() < p2.getY() ){
                return -1;
            }else{
                return 1;
            }
        }else if ( p1.getX() < p2.getX()){
            return -1;
        }else{
            return 1;
        }
    }

    @Override
    public int compareTo(Point point){
        return compare(this, point);
    }

}

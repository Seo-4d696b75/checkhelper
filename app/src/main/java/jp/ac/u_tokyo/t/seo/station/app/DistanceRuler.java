package jp.ac.u_tokyo.t.seo.station.app;


import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/04/25.
 */

abstract class DistanceRuler {

    /**
     *
     * @param distance measured in meters
     * @return
     */
    static String formatDistance(double distance){
        if ( distance < 1000 ){
            return String.format(Locale.US, "%.0fm", distance);
        }else{
            return String.format(Locale.US, "%.2fkm", distance / 1000.0);
        }
    }

    public double measure(Station station, double lon, double lat){
        return measure(station.longitude, station.latitude, lon, lat);
    }

    public double measureDistance(Station station, double lon, double lat){
        return measureDistance(station.longitude, station.latitude, lon, lat);
    }

    /**
     * 指定された二点間の距離の大小を表す係数値を計算する.
     * 返値の単位は各実装に依る
     * <strong>NOTE </strong>パラメータの値はすべてオイラー角
     * @return not in meters
     */
    public abstract double measure(double lon1, double lat1, double lon2, double lat2);

    /**
     * 指定された二点間の距離を計算する.
     * 単位はメートルで人間に分かりやすい表記とする.
     * ただし{@link #measure(Station, double, double)}で得られる値との対応は保証しない
     * <strong>NOTE </strong>この値で大小を比較するな!
     * @return in meters
     */
    public abstract double measureDistance(double lon1, double lat1, double lon2, double lat2);

    public static DistanceRuler getInstance(boolean isHigh){
        if ( isHigh ){
            return new SphereRuler();
        }else{
            return new PythagoreanRuler();
        }
    }

    /**
     * 地球を球体と見なしたときの平均半径(m)
     */
    private static final double RADIUS = 6378137.0;

    /**
     * 地球表面を緯度・経度方向ベクトルが張るガウス平面に射影したときの距離を測る.
     * = 緯度・経度の値に関してユークリッド距離を計算する.
     * 緯度が高いほど、二点が東西方向に並んでいるほど実際との距離の誤差は大きくなる.
     */
    private static class PythagoreanRuler extends DistanceRuler{

        @Override
        public double measure(double lon1, double lat1, double lon2, double lat2){
            return Math.sqrt(Math.pow(lon1-lon2, 2) + Math.pow(lat1-lat2, 2));
        }

        @Override
        public double measureDistance(double lon1, double lat1, double lon2, double lat2){
            lon1 = Math.toRadians(lon1);
            lon2 = Math.toRadians(lon2);
            lat1 = Math.toRadians(lat1);
            lat2 = Math.toRadians(lat2);
            double lat = RADIUS * Math.abs(lat1 - lat2);
            double lon = RADIUS * Math.cos((lat1 + lat2) / 2) * Math.abs(lon1 - lon2);
            return Math.sqrt(lat*lat + lon*lon);
        }
    }

    /**
     * 地球を完全な球面と仮定したときの距離(測地線の長さ・最短距離)を測る.
     */
    private static class SphereRuler extends DistanceRuler{

        @Override
        public double measure(double lon1, double lat1, double lon2, double lat2){
            lon1 = Math.toRadians(lon1);
            lon2 = Math.toRadians(lon2);
            lat1 = Math.toRadians(lat1);
            lat2 = Math.toRadians(lat2);
            double lon = (lon1 - lon2) / 2;
            double lat = (lat1 - lat2) / 2;
            return Math.asin(Math.sqrt(Math.pow(Math.sin(lat), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(lon), 2)));
        }

        @Override
        public double measureDistance(double lon1, double lat1, double lon2, double lat2){
            return RADIUS * 2 * measure(lon1, lat1, lon2, lat2);
        }

    }

}

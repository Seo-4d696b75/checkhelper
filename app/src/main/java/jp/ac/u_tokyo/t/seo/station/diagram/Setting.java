package jp.ac.u_tokyo.t.seo.station.diagram;

/**
 * @author Seo-4d696b75
 * @version 2018/05/13
 */
public final class Setting {

    private Setting(){}

    public static double error = Math.pow(2,-40);

    public static boolean isZero(double value){
        return Math.abs(value) <= error;
    }

    public static boolean greaterThanZero(double value){
        return value >= -error;
    }

    public static boolean lessThanZero(double value){
        return value <= error;
    }

}

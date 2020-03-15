package jp.ac.u_tokyo.t.seo.station.app;

/**
 * @author Seo-4d696b75
 * @version 2019/10/16.
 */
public class AppException extends RuntimeException{


    AppException(String mes, Throwable cause){
        super(mes, cause);
    }

    AppException(String mes){
        super(mes);
    }


}

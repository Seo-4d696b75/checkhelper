package jp.ac.u_tokyo.t.seo.station.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/05/02.
 */

class LogHolder{

    private static final String DATE_FORMAT = "HH:mm:ss.SSS";

    static final int LOG_FILTER_ALL = 0b111;
    static final int LOG_FILTER_GEO = 0b110;
    static final int LOG_SYSTEM = 0b001;
    static final int LOG_LOCATION = 0b010;
    static final int LOG_STATION = 0b100;

    private List<ServiceLog> mList;
    private boolean mHasError = false;

    LogHolder(){
        mList = new LinkedList<>();
        Date date = new Date();
        String mes = "app starts on " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(date);
        append(mes, false);
    }

    synchronized void append(String message, boolean error){
        mList.add(new ServiceLog(message));
        mHasError = mHasError | error;
    }

    boolean hasError(){
        return mHasError;
    }

    synchronized void append(double lon, double lat, Station station){
       mList.add(new ServiceLog(lon, lat, station));
    }

    @Override
    public synchronized String toString(){
        StringBuilder builder = new StringBuilder();
        for ( Iterator<ServiceLog> iterator = mList.iterator() ; iterator.hasNext() ; ){
            builder.append(iterator.next().toString());
            if ( iterator.hasNext() ){
                builder.append("\n");
            }
        }
        return builder.toString();
    }


    List<ServiceLog> getLogList(int filter){
        List<ServiceLog> list = new LinkedList<>();
        for ( ServiceLog item : mList ){
            if ( ( item.mType & filter ) > 0 ){
                list.add(item);
            }
        }
        return list;
    }

    ServiceLog getLatest(){
        return mList.get(mList.size()-1);
    }


    static class ServiceLog{

        private ServiceLog(String message){
            mType = LOG_SYSTEM;
            mTime = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date());
            mMessage = message;
        }

        private ServiceLog(double lon, double lat, Station station){
            mType = station == null ? LOG_LOCATION : LOG_STATION;
            mTime = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date());
            mMessage = station == null ? String.format(Locale.US, "(%.6f,%.6f)", lon, lat) :
                    String.format(Locale.US,"%s(%d)", station.name, station.code);
        }

        final String mTime;
        final String mMessage;
        final int mType;


        @Override
        public String toString(){
            return mTime + " " + mMessage;
        }

    }

}

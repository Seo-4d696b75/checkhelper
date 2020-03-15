package jp.ac.u_tokyo.t.seo.station.app;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/11/03.
 */

class Station{

    interface LineAccessor{
        @NonNull
        Line getLine(int id);

    }

    Station(JSONObject data, LineAccessor accessor) throws JSONException{
        code = data.getInt("code");
        name = data.getString("name");
        longitude = data.getDouble("lng");
        latitude = data.getDouble("lat");
        nameKana = data.getString("name_kana");
        prefecture = data.getInt("prefecture");

        JSONArray array = data.getJSONArray("lines");
        this.lines = new Line[array.length()];
        for ( int i = 0; i < array.length(); i++ ){
            int code = array.getInt(i);
            this.lines[i] = accessor.getLine(code);
            if ( this.lines[i] == null ){
                this.lines[i] = new Line(code);
            }
        }

        array = data.getJSONArray("next");
        next = new int[array.length()];
        for ( int i = 0; i < array.length(); i++ ){
            next[i] = array.getInt(i);
        }
        mAreaData = new StationMapFragment.StationArea(this, data.getJSONObject("voronoi"));
    }

    Station(int code){
        this.code = code;
        name = "unknown";
        nameKana = "ここではないどこか";
        longitude = 0;
        latitude = 0;
        prefecture = 1;
        lines = new Line[0];
        next = new int[0];
        mAreaData = null;
    }

    final int code;
    final double longitude;
    final double latitude;
    final String name;
    final String nameKana;
    final int prefecture;
    final Line[] lines;
    final int[] next;

    @Override
    public boolean equals(Object other){
        return other instanceof Station && this.code == ((Station)other).code;
    }

    String getLinesName(){
        String mes = "";
        for ( Line line : lines ){
            String name = line.mName;
            if ( name != null ){
                mes += name + " ";
            }
        }
        return mes;
    }

    boolean isLine(Line line){
        if ( line != null ){
            for ( Line item : lines ){
                if ( line.equals(item) ){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString(){
        return String.format(Locale.US, "Station{name:%s, pos:(%.4f,%.4f)}", name, longitude, latitude);
    }

    final StationMapFragment.StationArea mAreaData;

}

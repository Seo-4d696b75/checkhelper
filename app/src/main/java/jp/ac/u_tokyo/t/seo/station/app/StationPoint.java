package jp.ac.u_tokyo.t.seo.station.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import jp.ac.u_tokyo.t.seo.station.diagram.Point;

/**
 * @author Seo-4d696b75
 * @version 2019/06/20.
 * 駅オブジェクトを座標点として扱うためのラッパークラス.
 * 同値判定{@link Object#equals(Object)},{@link Object#hashCode()}を正しく実装する必要あり.
 */
class StationPoint extends Point{

    StationPoint(JSONObject data) throws JSONException{
        lon = data.getDouble("lng");
        lat = data.getDouble("lat");
        code = data.getInt("code");
        name = data.getString("name");
        JSONArray array = data.getJSONArray("next");
        next = new int[array.length()];
        for ( int i = 0; i < array.length(); i++ ){
            next[i] = array.getInt(i);
        }
    }

    StationPoint(Station station){
        lon = station.longitude;
        lat = station.latitude;
        name = station.name;
        code = station.code;
        next = station.next;
    }

    final double lon,lat;
    final int code;
    final String name;
    final int[] next;

    @Override
    public double getX(){
        return lon;
    }

    @Override
    public double getY(){
        return lat;
    }

    @Override
    public String toString(){
        return String.format(Locale.US, "StationPoint{code:%d name:%s lat/lng:%.6f/%.6f}", code, name, lat, lon);
    }
}

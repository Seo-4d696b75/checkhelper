package jp.ac.u_tokyo.t.seo.station.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.ac.u_tokyo.t.seo.station.diagram.Point;

/**
 * @author Seo-4d696b75
 * @version 2019/06/20.
 */
class StationPoint extends Point{

    StationPoint(JSONObject data) throws JSONException{
        lon = data.getDouble("lon");
        lat = data.getDouble("lat");
        id = data.getInt("code");
        name = data.getString("station");
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
        id = station.code;
        next = station.next;
    }

    final double lon,lat;
    final int id;
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

}

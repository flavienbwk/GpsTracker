package com.berwick.gpstracker;

import android.location.Location;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;


class LocationSerializer implements JsonSerializer<Location> {
    public JsonElement serialize(Location t, Type type,
                                 JsonSerializationContext jsc) {
        JsonObject jo = new JsonObject();
        jo.addProperty("mProvider", t.getProvider());
        jo.addProperty("mTime", t.getTime());
        jo.addProperty("mAccuracy", t.getAccuracy());
        jo.addProperty("mLongitude", t.getLongitude());
        jo.addProperty("mLatitude", t.getLatitude());
        jo.addProperty("mAltitude", t.getAltitude());
        jo.addProperty("mSpeed", t.getSpeed());
        jo.addProperty("mBearing", t.getBearing());
        return jo;
    }

}

class LocationDeserializer implements JsonDeserializer<Location> {
    public Location deserialize(JsonElement je, Type type,
                                JsonDeserializationContext jdc)
            throws JsonParseException {
        JsonObject jo = je.getAsJsonObject();
        if (jo != null) {
            if (jo.getAsJsonPrimitive("mAccuracy") != null) {
                Location l = new Location(jo.getAsJsonPrimitive("mProvider").getAsString());
                l.setTime(jo.getAsJsonPrimitive("mTime").getAsLong());
                l.setAccuracy(jo.getAsJsonPrimitive("mAccuracy").getAsFloat());
                l.setLongitude(jo.getAsJsonPrimitive("mLongitude").getAsFloat());
                l.setLatitude(jo.getAsJsonPrimitive("mLatitude").getAsFloat());
                l.setAltitude(jo.getAsJsonPrimitive("mAltitude").getAsFloat());
                l.setSpeed(jo.getAsJsonPrimitive("mSpeed").getAsFloat());
                l.setBearing(jo.getAsJsonPrimitive("mBearing").getAsFloat());
                return l;
            }
        }
        return null;
    }
}
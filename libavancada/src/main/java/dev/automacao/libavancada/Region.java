package dev.automacao.libavancada;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

public class Region {
    private final String name;
    private final Long timestamp;
    private final Double latitude;
    private final Double longitude;
    private final Long user;

    public Region(String name, Location location) {
        this.name = name;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.timestamp = System.currentTimeMillis();
        this.user = 0L;
    }

    public Region(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.latitude = (Double) map.get("latitude");
        this.longitude = (Double) map.get("longitude");
        this.timestamp = (Long) map.get("timestamp");
        this.user = (Long) map.get("user");
    }

    public String getName() {
        return name;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Long getUser() {
        return user;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Boolean canEnqueue(Region r1) {
        return LocationMath.distance(r1, this) >= 30;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("timestamp", timestamp);
        map.put("user", user);
        map.put("latitude", getLatitude());
        map.put("longitude", getLongitude());
        return map;
    }
}

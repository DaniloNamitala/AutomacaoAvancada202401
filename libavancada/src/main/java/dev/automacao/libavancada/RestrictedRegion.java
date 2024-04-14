package dev.automacao.libavancada;

import android.location.Location;

import java.util.Map;

public class RestrictedRegion extends Region {
    private Region mainRegion;

    public RestrictedRegion(String name, Location location, Region mainRegion) {
        super(name, location);
        this.mainRegion = mainRegion;
    }

    public RestrictedRegion(Map<String, Object> map, Region mainRegion) {
        super(map);
        this.mainRegion = mainRegion;
    }

    @Override
    public Boolean canEnqueue(Region r1) {
        return LocationMath.distance(r1, this) >= 5;
    }
}

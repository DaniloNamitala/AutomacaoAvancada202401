package dev.automacao.libavancada;

import android.location.Location;

public class LocationMath {
    
    public static float distance(Region r1, Region r2) {
        float[] result = new float[1];
        Location.distanceBetween(r1.getLatitude(), r1.getLongitude(), r2.getLatitude(), r2.getLongitude(), result);
        return result[0];
    }
}

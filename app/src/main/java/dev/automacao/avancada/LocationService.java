package dev.automacao.avancada;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class LocationService {
    ReentrantLock lock = new ReentrantLock();
    private final Queue<Location> queue = new LinkedList<>();
    private final FusedLocationProviderClient locationClient;
    public LocationService(FusedLocationProviderClient locationClient) {
        this.locationClient = locationClient;
    }

    public void getCurrentLocation(Context context, OnSuccessListener<? super Location> success) {
        new Thread(() -> { // executa em outra thread
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient.getLastLocation()
                        .addOnSuccessListener((Activity) context, success);
            }
        }).start();
    }

    public void enqueue(Location item){
        new Thread(() -> {
            try {
                lock.lock();
                queue.add(item);
            } finally {
                lock.unlock();
            }
        }).start();
    }

    public Location dequeue() {
        if (lock.tryLock()) {
            try {
                lock.lock();
                return queue.remove();
            } catch (NoSuchElementException e) {
                Log.e("QUEUE EXCEPTION", Objects.requireNonNull(e.getMessage()));
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
}

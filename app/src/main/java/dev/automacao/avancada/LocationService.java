package dev.automacao.avancada;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import dev.automacao.libavancada.LocationMath;
import dev.automacao.libavancada.Region;

public class LocationService {
    ReentrantLock lock = new ReentrantLock();
    private final Queue<Region> queue = new LinkedList<>();
    private final FusedLocationProviderClient locationClient;
    private int regionCount = 1;

    private final FirebaseFirestore db;

    public LocationService(FusedLocationProviderClient locationClient, FirebaseFirestore db) {
        this.db = db;
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

    private Boolean canAddDatabase(Region r1) {
        AtomicBoolean canAdd = new AtomicBoolean(true);
        db.collection("REGIONS")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("DATABASE", document.getId() + " => " + document.getData());
                            Region region = new Region(document.getData());
                            if (!LocationMath.canEnqueue(region, r1)) {
                                canAdd.set(false);
                                break;
                            }

                        }
                    } else {
                        Log.w("DATABASE", "Error getting documents.", task.getException());
                    }
                });
        return canAdd.get();
    }

    public void saveToDatabase() {
        new Thread(() -> {
            Region region = dequeue();
            if (region != null) {
                db.collection("REGIONS").document(region.getName()).set(region.toMap())
                        .addOnSuccessListener(unused -> {
                            Log.d("DATABASE", "Error adding document");
                        })
                        .addOnFailureListener(e -> Log.d("DATABASE", "Error adding document", e));

            }
        }).start();

    }

    public void enqueue(Location location){
        Region region = new Region("Region"+regionCount, location);
        AtomicBoolean canAddQueue = new AtomicBoolean(false);
        AtomicBoolean canAddDatabase = new AtomicBoolean(false);

        Thread threadQueue = new Thread(() -> {
            canAddQueue.set(canEnqueue(region));
        });

        Thread threadDatabase = new Thread(() -> {
            canAddDatabase.set(canAddDatabase(region));
        });

        new Thread(() -> {
            try {
                threadDatabase.start();
                threadQueue.start();
                threadDatabase.join();
                threadQueue.join();

                if (canAddQueue.get() && canAddDatabase.get()) {
                    lock.lock();
                    queue.add(region);
                    regionCount++;
                } else {
                    Log.d("ENQUEUE", "nao adicionou");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (lock.isLocked())
                    lock.unlock();
            }
        }).start();
    }

    private Boolean canEnqueue(Region r1) {
        LinkedList<Region> list = (LinkedList<Region>) queue;
        for (Region r : list) {
            if (!LocationMath.canEnqueue(r1, r)) {
                return false;
            }
        }
        return true;
    }

    public Region dequeue() {
        Region region = null;
        if (lock.tryLock()) {
            try {
                lock.lock();
                region = queue.remove();
            } catch (NoSuchElementException e) {
                Log.e("QUEUE EXCEPTION", "NAO HA OBJETOS NA FILA");
            } finally {
                if (lock.isLocked())
                    lock.unlock();
            }
        }
        return region;
    }
}

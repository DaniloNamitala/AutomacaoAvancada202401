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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import dev.automacao.libavancada.LocationMath;
import dev.automacao.libavancada.Region;
import dev.automacao.libavancada.RestrictedRegion;
import dev.automacao.libavancada.SubRegion;

public class LocationService {
    ReentrantLock lock = new ReentrantLock();
    private final Queue<Region> queue = new LinkedList<>();
    private final FusedLocationProviderClient locationClient;
    private Integer regionCount = 0;
    private final FirebaseFirestore db;
    private Boolean createSubRegion = false;

    public LocationService(FusedLocationProviderClient locationClient, FirebaseFirestore db) {
        this.db = db;
        this.locationClient = locationClient;
        readCount();
    }

    public void getCurrentLocation(Context context, OnSuccessListener<? super Location> success) {
        new Thread(() -> { // executa em outra thread
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient.getLastLocation()
                        .addOnSuccessListener((Activity) context, success);
            }
        }).start();
    }

    private Thread canAddDatabase(Region r1, AtomicBoolean canAdd) {
        return new Thread(() -> {
            Task<QuerySnapshot> task = db.collection("REGIONS").get();
            canAdd.set(true);
            try {
                while (!(task.isComplete() || task.isCanceled()));
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("AVANCADA", document.getId() + " => " + document.getData());
                            Region region = new Region(document.getData());
                            if (!region.canEnqueue(r1)) {
                                canAdd.set(false);
                                break;
                            }
                        }
                    }
                } else {
                    Log.d("DATABASE", "Error getting documents.", task.getException());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void saveToDatabase() {
        new Thread(() -> {
            Region region = dequeue();
            if (region != null) {
                db.collection("REGIONS").document(region.getName()).set(region.toMap())
                        .addOnSuccessListener(unused -> {
                            Log.d("AVANCADA", "DOCUMENTO ADICIONADO COM SUCESSO");

                            db.collection("CONFIG").document("REGION_COUNT").update("COUNT", regionCount);
                        })
                        .addOnFailureListener(e -> Log.d("AVANCADA", "ERRO AO ADICIONAR DOCUMENTO", e));

            }
        }).start();
    }

    private void readCount() {
        db.collection("CONFIG").document("REGION_COUNT").get().addOnSuccessListener(documentSnapshot -> {
           if (documentSnapshot.get("COUNT") != null)
               regionCount = Math.toIntExact((Long) documentSnapshot.get("COUNT"));
        });
    }

    public void enqueue(Location location){
        Region region = new Region("Region"+regionCount, location);
        AtomicBoolean canAddQueue = new AtomicBoolean(true);
        AtomicReference<Region> mainRegion = new AtomicReference<>(null);
        AtomicBoolean canAddDatabase = new AtomicBoolean(true);

        Thread threadQueue = canEnqueue(region, canAddQueue, mainRegion);
        Thread threadDatabase = canAddDatabase(region, canAddDatabase);

        new Thread(() -> {
            try {
                threadDatabase.start();
                threadQueue.start();
                threadDatabase.join();
                threadQueue.join();

                if (canAddQueue.get() && canAddDatabase.get()) {
                    Region newRegion = new Region("Region"+regionCount, location);
                    if (mainRegion.get() != null) {
                        if (createSubRegion) {
                            newRegion = new RestrictedRegion("Region" + regionCount, location, mainRegion.get());
                            Log.d("AVANCADA", "CRIADO UMA RESTRICTED REGION");
                        } else {
                            newRegion = new SubRegion("Region" + regionCount, location, mainRegion.get());
                            Log.d("AVANCADA", "CRIADO UMA SUB REGION");
                        }
                        createSubRegion = !createSubRegion;
                    }

                    if (lock.tryLock()) {
                        queue.add(newRegion);
                        lock.unlock();
                        regionCount++;
                        Log.d("AVANCADA", "ADICIONADO NA FILA");
                    }
                } else {
                    Log.d("AVANCADA", "NAO ADICIONADO NA FILA");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private Thread canEnqueue(Region r1, AtomicBoolean canAdd, AtomicReference<Region> mainRegion) {
        return new Thread(() -> {
            canAdd.set(true);
            LinkedList<Region> list = (LinkedList<Region>) queue;
            for (Region r : list) {
                if ((r instanceof SubRegion) || (r instanceof RestrictedRegion)) { // é SubRegion ou RestrictedRegion
                    if (!r.canEnqueue(r1)) { // verifica se pode adicionar
                        canAdd.set(false);
                        break;
                    }
                } else { // é Region
                    if (!r.canEnqueue(r1)) // esta no raio de 30m
                        mainRegion.set(r);
                }

            }
        });
    }

    public Region dequeue() {
        Region region = null;
        if (lock.tryLock()) {
            region = queue.poll();
            lock.unlock();
        }
        return region;
    }
}

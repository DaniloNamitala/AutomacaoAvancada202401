package dev.automacao.avancada;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class AvancadaApp extends Application {

    private FirebaseFirestore database;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        database = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDatabase() {
        return database;
    }
}

package dev.automacao.avancada;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LastLocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

import dev.automacao.avancada.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private LocationService locationService;
    private Location lastLocation = null;
    private ActivityMainBinding binding;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationService = new LocationService(fusedLocationClient);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.mapView.setTileSource(new XYTileSource("OSM", 0, 17, 512, ".png",
                new String[]{"https://tile.openstreetmap.de/"}) {

        });
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    binding.latitude.setText(String.format("LAT: %s", locationResult.getLastLocation().getLatitude()));
                    binding.longitude.setText(String.format("LONG: %s", locationResult.getLastLocation().getLongitude()));
                    lastLocation = locationResult.getLastLocation();
                    setMarker(lastLocation);
                    goToPoint(lastLocation);
                    setZoom(15.0);
                }
            }
        };

        createLocationRequest();
        requestPermissionsIfNecessary(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        setListeners();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(500)
                .setIntervalMillis(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();

        getLocation();
        goToPoint(lastLocation);
        IMapController mapController = binding.mapView.getController();
        mapController.setZoom(12.0);

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.mapView.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void setListeners() {
        binding.btnLocal.setOnClickListener(v -> {
            getLocation();
            goToPoint(lastLocation);
            setMarker(lastLocation);
            setZoom(19.0);
        });

        binding.btnEnqueue.setOnClickListener(v -> {
            if (lastLocation != null) {
                locationService.enqueue(lastLocation);
            }
        });
    }

    private void goToPoint(Location location) {
        if (location == null) return;

        IMapController mapController = binding.mapView.getController();
        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.setCenter(startPoint);
    }

    private void setZoom(double zoom) {
        IMapController mapController = binding.mapView.getController();
        mapController.zoomTo(zoom);
    }

    private void setMarker(Location location) {
        if (location != null) {
            Marker startMarker;
            if (binding.mapView.getOverlays().isEmpty()) {
                startMarker = new Marker(binding.mapView);
                startMarker.setIcon(AppCompatResources.getDrawable(this, R.drawable.icon_navigation));
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                binding.mapView.getOverlays().add(startMarker);
            } else {
                startMarker = (Marker) binding.mapView.getOverlays().get(0);
            }
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            startMarker.setPosition(startPoint);
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.getCurrentLocation(this, location -> {
                if (location != null) {
                    binding.latitude.setText(String.format("LAT: %s", location.getLatitude()));
                    binding.longitude.setText(String.format("LONG: %s", location.getLongitude()));
                    lastLocation = location;
                }
            });
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
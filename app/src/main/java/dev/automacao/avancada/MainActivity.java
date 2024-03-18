package dev.automacao.avancada;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationService = new LocationService(fusedLocationClient);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.mapView.setTileSource(new XYTileSource("OSM", 0, 17, 512, ".png",
                new String[] { "https://tile.openstreetmap.de/" }) {

        });

        requestPermissionsIfNecessary(new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        setListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();

        getLocation();
        goToPoint(lastLocation);
        IMapController mapController = binding.mapView.getController();
        mapController.setZoom(12.0);

    }

    @Override
    public void onPause() {
        super.onPause();
        binding.mapView.onPause();
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
        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Marker startMarker = new Marker(binding.mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        binding.mapView.getOverlays().add(startMarker);
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
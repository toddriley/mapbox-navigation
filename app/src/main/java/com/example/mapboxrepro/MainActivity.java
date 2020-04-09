package com.example.mapboxrepro;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements
        ActivityResultListener,
        MapboxMap.OnMapClickListener,
        OnMapReadyCallback,
        PermissionsListener {
    private static final String TAG = "MainActivity";
    private static final int SHOW_ROUTE_BOUNDS_PADDING = 75;
    private static final int SHOW_ROUTE_EASE_DURATION = 1000;
    public static final int ARRIVAL_LOCATION_REQUEST_ID = 0;
    private static final Point ORIGIN = Point.fromLngLat(-77.5659408569336, 37.605369567871094);
    private static final Point DESTINATION = Point.fromLngLat(-77.5505277, 37.461559);

    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private DirectionsRoute currentRoute;
    private RouteRequest routeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for location permission
        permissionsManager = new PermissionsManager(this);
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            permissionsManager.requestLocationPermissions(this);
        } else {
            requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
        }

        routeRequest = new RouteRequest(ORIGIN, DESTINATION);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    /**
     * Permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 0) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, "You didn't grant storage permissions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "This app needs location and storage permissions"
                + "in order to show its functionality.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
        } else {
            Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissionIfNotGranted(String permission) {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(permission);
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 10);
        }
    }

    /**
     * Maps
     */
    @Override
    public boolean onMapClick(LatLng point) {
        Log.i(TAG, "Clicked");
        Intent intent = new Intent(getApplicationContext(), EmbeddedNavigationActivity.class);
        intent.putExtra(EmbeddedNavigationActivity.BUNDLE_CURRENT_ROUTE, currentRoute);
        intent.putExtra(EmbeddedNavigationActivity.BUNDLE_ROUTE_REQUEST, routeRequest);
        startActivityForResult(intent, ARRIVAL_LOCATION_REQUEST_ID);
        return true;
    }

    @Override
    public void onCustomActivityResult(int requestCode, int resultCode, Intent bundle) {
        Log.i("Main", "Result received");
        if (requestCode == ARRIVAL_LOCATION_REQUEST_ID) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Activity completed successfully");
            }
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Log.i("Main","onMapReady called");
        this.mapboxMap = mapboxMap;
        updateCameraView();
        renderMapMarkers();
//        disableMapInteraction();

    }

    private void renderMapMarkers() {
        Timber.v("Building map marker style");
        Resources resources = getResources();
        Feature origin= Feature.fromGeometry(ORIGIN);
        Feature destination= Feature.fromGeometry(DESTINATION);
        List<Feature> markerList = new ArrayList<>();
        markerList.add(origin);
        markerList.add(destination);

        Style.Builder styleBuilder = MapDecorator.getStyleBuilderWithStops(
                    resources,
                    markerList);

        mapboxMap.setStyle(styleBuilder, style -> {
            MapBoxClient.getInstance().buildRoute(getApplicationContext(),
                    routeRequest,
                    route -> {
                        currentRoute = route;
                        mapboxMap.getStyle(this::renderMapLine);
                    });
        });
    }

    private void updateCameraView() {
        if (mapboxMap != null) {
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(ORIGIN.latitude(), ORIGIN.longitude()))
                    .zoom(12.5)
                    .build()));
        }
    }

//    private void disableMapInteraction() {
//        UiSettings settings = mapboxMap.getUiSettings();
//        settings.setAllGesturesEnabled(false);
//    }

    private void renderMapLine(Style style) {
        NavigationMapRoute navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.MapViewRoute);
        navigationMapRoute.addRoute(currentRoute);
        showEntireDirectionsRoute();
        Log.i(TAG, "Setting on click listener");
        mapboxMap.addOnMapClickListener(this);
    }

    private void showEntireDirectionsRoute() {
        if (mapboxMap == null || currentRoute == null) {
            return;
        }
        LatLngBounds routeBounds = MapDecorator.getRouteBounds(currentRoute);
        mapboxMap.easeCamera(
                CameraUpdateFactory.newLatLngBounds(routeBounds, SHOW_ROUTE_BOUNDS_PADDING),
                SHOW_ROUTE_EASE_DURATION);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
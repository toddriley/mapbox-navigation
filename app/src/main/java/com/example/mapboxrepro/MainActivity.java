package com.example.mapboxrepro;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements
        MapboxMap.OnMapClickListener,
        OnMapReadyCallback,
        PermissionsListener {
    private static final String TAG = "MainActivity";
    private static final Point ORIGIN = Point.fromLngLat(-77.5659408569336, 37.605369567871094);
    private static final Point DESTINATION = Point.fromLngLat(-77.5505277, 37.461559);
    private static final String ENDPOINT_ICON_KEY = "endpoint_icon";
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String STYLE_URI = "mapbox://styles/mapbox/navigation-guidance-day-v3";
    private static final String LAYER_ID = "LAYER_ID";

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private DirectionsRoute currentRoute;
    private RouteRequest routeRequest;
    private boolean navigationIsReady = false;

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
        if (navigationIsReady) {
            Intent intent = new Intent(getApplicationContext(), EmbeddedNavigationActivity.class);
            intent.putExtra(EmbeddedNavigationActivity.BUNDLE_CURRENT_ROUTE, currentRoute);
            intent.putExtra(EmbeddedNavigationActivity.BUNDLE_ROUTE_REQUEST, routeRequest);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Log.i("Main", "onMapReady called");
        fetchRoute();

        Drawable endpointIcon = getResources().getDrawable(R.drawable.ic_aap_flag_no_circle_black, null);

        Style.Builder styleBuilder = new Style.Builder().fromUri(STYLE_URI)
                .withImage(ENDPOINT_ICON_KEY, endpointIcon)
                .withLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                        .withProperties(
                                PropertyFactory.iconImage(Expression.literal(ENDPOINT_ICON_KEY)),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.textColor(Color.WHITE),
                                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_CENTER),
                                PropertyFactory.textFont(new String[]{"Open Sans Bold"})
                        )
                );

        mapboxMap.setStyle(styleBuilder, style -> {
            fetchRoute();
        });

        mapboxMap.addOnMapClickListener(this);
    }

    private void fetchRoute() {
        NavigationRoute.Builder routeBuilder = NavigationRoute.builder(getApplicationContext())
                .accessToken(Mapbox.getAccessToken())
                .origin(routeRequest.getOrigin());

        routeBuilder.destination(routeRequest.getDestination())
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        navigationIsReady = true;
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: %s", throwable);
                    }
                });
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
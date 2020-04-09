package com.example.mapboxrepro;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mapboxrepro.R;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;

import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class MapViewWrapper extends MapView implements OnMapReadyCallback {
    private static final int SHOW_ROUTE_BOUNDS_PADDING = 75;
    private static final int SHOW_ROUTE_EASE_DURATION = 1000;

    private Context context;
    private boolean isDestroyed;
    private boolean isPaused;

    private MapboxMap mapboxMap;

    private Double latitude;
    private Double longitude;
    private List<LatLng> waypoints;
    private Double zoom;

    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    public MapViewWrapper(Context context, MapboxMapOptions options) {
        super(context, options);

        this.context = context;
        onCreate(null);
        onStart();
        onResume();
        getMapAsync(this);
    }

    private void createRoute() {
        if (waypoints.size() == 0) {
            return;
        }

        tryRenderMapMarkers();

        int pointCount = waypoints.size();
        if (pointCount >= 2) {
            LatLng destination = waypoints.remove(pointCount - 1);
            LatLng origin = waypoints.remove(0);
            RouteRequest routeRequest = new RouteRequest(Point.fromLngLat(origin.getLongitude(), origin.getLatitude()), Point.fromLngLat(destination.getLongitude(), destination.getLatitude()));
            MapBoxClient.getInstance().buildRoute(getContext(),
                    routeRequest,
                    route -> {
                        currentRoute = route;
                        if (mapboxMap != null) {
                            mapboxMap.getStyle(this::tryRenderMapLine);
                        }
                        showEntireDirectionsRoute();
                    });
        }
    }

    private void tryRenderMapMarkers() {
        if (mapboxMap != null) {
            Timber.v("Building map marker style");
            Style.Builder styleBuilder;
            Resources resources = context.getResources();
            if (!waypoints.isEmpty()) {
                List<Feature> undrawnMapMarkerList = waypoints.stream()
                        .map(point -> {
                            Feature feature = Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude()));
                            return feature;
                        })
                        .collect(Collectors.toList());
                styleBuilder = MapDecorator.getStyleBuilderWithStops(
                        resources,
                        undrawnMapMarkerList);
            } else {
                styleBuilder = MapDecorator.getStyleBuilder(resources);
            }
            mapboxMap.setStyle(styleBuilder, style -> {
                tryRenderMapLine(style);
                showEntireDirectionsRoute();
            });
        }
    }

    private void tryRenderMapLine(Style style) {
        if (mapboxMap != null && currentRoute != null) {
            if (navigationMapRoute != null) {
                navigationMapRoute.updateRouteVisibilityTo(true);
                navigationMapRoute.updateRouteArrowVisibilityTo(false);
                navigationMapRoute.addRoute(currentRoute);
            } else if (style != null) {
                navigationMapRoute = new NavigationMapRoute(null, this, mapboxMap, R.style.MapViewRoute);
                navigationMapRoute.addRoute(currentRoute);
            } else {
                Timber.v("Style not set when trying to render the map line. Constructing a 'NavigationMapRoute' requires a 'mapboxMap' with its style loaded.");
            }
        } else {
            Timber.v("mapboxMap not initialized when trying to render map line.");
        }
    }

    private void updateCameraView() {
        if (mapboxMap != null && latitude != null && longitude != null && zoom != null) {
            LatLng target = new LatLng(latitude, longitude);
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(target)
                    .zoom(zoom)
                    .build()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.v("MapViewWrapper activity destroyed");
        isDestroyed = true;
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        Timber.i("onMapReady called");
        this.mapboxMap = mapboxMap;
        updateCameraView();
        tryRenderMapMarkers();
        disableMapInteraction();
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.v("MapViewWrapper activity paused");
        isPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.v("MapViewWrapper activity resumed");
        isPaused = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        // MainActivity started, Navigation stopped
        Timber.v("MapViewWrapper activity started");
    }

    @Override
    public void onStop() {
        super.onStop();
        // MainActivity stopped, Navigation started
        Timber.v("MapViewWrapper activity stopped");
    }

    private void disableMapInteraction() {
        UiSettings settings = mapboxMap.getUiSettings();
        settings.setAllGesturesEnabled(false);
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
}
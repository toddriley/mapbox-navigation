package com.example.mapboxrepro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class EmbeddedNavigationActivity extends AppCompatActivity implements
        OnNavigationReadyCallback,
        NavigationListener,
        ProgressChangeListener,
        InstructionListListener,
        RouteListener,
        BannerInstructionsListener {
    private static final int INITIAL_ZOOM = 16;
    public static final String BUNDLE_CURRENT_ROUTE = "currentRoute";
    public static final String BUNDLE_ROUTE_REQUEST = "routeRequest";

    private NavigationView navigationView;
    private View spacer;
    private TextView speedWidget;
    private FloatingActionButton fabNightModeToggle;

    private boolean bottomSheetVisible = true;
    private boolean instructionListShown = false;

    private DirectionsRoute currentRoute;
    private RouteRequest routeRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        initNightMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embedded_navigation);
        navigationView = findViewById(R.id.navigationView);
        fabNightModeToggle = findViewById(R.id.fabToggleNightMode);
        speedWidget = findViewById(R.id.speed_limit);
        spacer = findViewById(R.id.spacer);
        setSpeedWidgetAnchor(R.id.summaryBottomSheet);
        Intent intent = getIntent();
        currentRoute = (DirectionsRoute) intent.getSerializableExtra(BUNDLE_CURRENT_ROUTE);
        routeRequest = (RouteRequest) intent.getSerializableExtra(BUNDLE_ROUTE_REQUEST);
        Point origin = routeRequest.getOrigin();
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(origin.latitude(), origin.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        startNavigation();
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
// If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        if (isFinishing()) {
            saveNightModeToPreferences(AppCompatDelegate.MODE_NIGHT_AUTO);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    @Override
    public void onCancelNavigation() {
// Navigation canceled, finish the activity
        finish();
    }

    @Override
    public void onNavigationFinished() {
// Intentionally empty
    }

    @Override
    public void onNavigationRunning() {
// Intentionally empty
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        setSpeed(location);
    }

    @Override
    public void onInstructionListVisibilityChanged(boolean shown) {
        instructionListShown = shown;
        speedWidget.setVisibility(shown ? View.GONE : View.VISIBLE);
        if (instructionListShown) {
            fabNightModeToggle.hide();
        } else if (bottomSheetVisible) {
            fabNightModeToggle.show();
        }
    }

    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {
        return instructions;
    }

    private void startNavigation() {
        NavigationViewOptions.Builder options =
                NavigationViewOptions.builder()
                        .navigationListener(this)
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(true)
                        .progressChangeListener(this)
                        .instructionListListener(this)
                        .routeListener(this)
                        .bannerInstructionsListener(this);
        setBottomSheetCallback(options);
        setupNightModeFab();

        navigationView.startNavigation(options.build());
    }

    /**
     * Sets the anchor of the spacer for the speed widget, thus setting the anchor for the speed widget
     * (The speed widget is anchored to the spacer, which is there because padding between items and
     * their anchors in CoordinatorLayouts is finicky.
     *
     * @param res resource for view of which to anchor the spacer
     */
    private void setSpeedWidgetAnchor(@IdRes int res) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacer.getLayoutParams();
        layoutParams.setAnchorId(res);
        spacer.setLayoutParams(layoutParams);
    }

    private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
        options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        bottomSheetVisible = false;
                        fabNightModeToggle.hide();
                        setSpeedWidgetAnchor(R.id.recenterBtn);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        bottomSheetVisible = true;
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        if (!bottomSheetVisible) {
// View needs to be anchored to the bottom sheet before it is finished expanding
// because of the animation
                            fabNightModeToggle.show();
                            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
                        }
                        break;
                    default:
                        return;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    private void setupNightModeFab() {
        fabNightModeToggle.setOnClickListener(view -> toggleNightMode());
    }

    private void toggleNightMode() {
        int currentNightMode = getCurrentNightMode();
        alternateNightMode(currentNightMode);
    }

    private void initNightMode() {
        int nightMode = retrieveNightModeFromPreferences();
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private int getCurrentNightMode() {
        return getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
    }

    private void alternateNightMode(int currentNightMode) {
        int newNightMode;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            newNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        saveNightModeToPreferences(newNightMode);
        recreate();
    }

    private int retrieveNightModeFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getInt(getString(R.string.current_night_mode), AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    private void saveNightModeToPreferences(int nightMode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.current_night_mode), nightMode);
        editor.apply();
    }

    private void setSpeed(Location location) {
        String string = String.format("%d\nMPH", (int) (location.getSpeed() * 2.2369));
        int mphTextSize = getResources().getDimensionPixelSize(R.dimen.mph_text_size);
        int speedTextSize = getResources().getDimensionPixelSize(R.dimen.speed_text_size);

        SpannableString spannableString = new SpannableString(string);
        spannableString.setSpan(new AbsoluteSizeSpan(mphTextSize),
                string.length() - 4, string.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        spannableString.setSpan(new AbsoluteSizeSpan(speedTextSize),
                0, string.length() - 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        speedWidget.setText(spannableString);
        if (!instructionListShown) {
            speedWidget.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean allowRerouteFrom(Point offRoutePoint) {
        return true;
    }

    @Override
    public void onOffRoute(Point offRoutePoint) {

    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {

    }

    @Override
    public void onFailedReroute(String errorMessage) {

    }

    @Override
    public void onArrival() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(BUNDLE_ROUTE_REQUEST, routeRequest);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
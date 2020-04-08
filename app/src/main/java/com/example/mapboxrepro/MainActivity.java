package com.example.mapboxrepro;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements ActivityResultListener, PermissionsListener {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private PermissionsManager permissionsManager;
    public static final int ARRIVAL_LOCATION_REQUEST_ID = 0;
    private static final Point ORIGIN = Point.fromLngLat(-77.5659408569336, 37.605369567871094);
    private static final Point DESTINATION = Point.fromLngLat(-77.5505277, 37.461559);

    private DirectionsRoute currentRoute;
    private RouteRequest routeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final List<SampleItem> samples = new ArrayList<>(Arrays.asList(
                new SampleItem(
                        getString(R.string.title_embedded_navigation),
                        getString(R.string.description_embedded_navigation),
                        EmbeddedNavigationActivity.class
                )
        ));

        // RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Specify an adapter
        RecyclerView.Adapter adapter = new MainAdapter(samples);
        recyclerView.setAdapter(adapter);

        // Check for location permission
        permissionsManager = new PermissionsManager(this);
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            recyclerView.setVisibility(View.INVISIBLE);
            permissionsManager.requestLocationPermissions(this);
        } else {
            requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
        }

        routeRequest = new RouteRequest(ORIGIN, DESTINATION);
        fetchRoute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 0) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                recyclerView.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "You didn't grant storage permissions.", Toast.LENGTH_LONG).show();
            } else {
                recyclerView.setVisibility(View.VISIBLE);
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

    /*
     * Recycler view
     */

    private class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

        private List<SampleItem> samples;

        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView nameView;
            private TextView descriptionView;

            ViewHolder(View view) {
                super(view);
                nameView = view.findViewById(R.id.nameView);
                descriptionView = view.findViewById(R.id.descriptionView);
            }
        }

        MainAdapter(List<SampleItem> samples) {
            this.samples = samples;
        }

        @NonNull
        @Override
        public MainAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.item_main_feature, parent, false);

            view.setOnClickListener(clickedView -> {
                if (currentRoute != null) {
                    int position = recyclerView.getChildLayoutPosition(clickedView);
                    Intent intent = new Intent(clickedView.getContext(), samples.get(position).getActivity());
                    intent.putExtra(EmbeddedNavigationActivity.BUNDLE_CURRENT_ROUTE, currentRoute);
                    intent.putExtra(EmbeddedNavigationActivity.BUNDLE_ROUTE_REQUEST, routeRequest);
                    intent.putExtra("origin", currentRoute);
                    startActivityForResult(intent, ARRIVAL_LOCATION_REQUEST_ID);
                } else {
                    Log.w(TAG, "currentRoute is null");
                }
            });

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MainAdapter.ViewHolder holder, int position) {
            holder.nameView.setText(samples.get(position).getName());
            holder.descriptionView.setText(samples.get(position).getDescription());
        }

        @Override
        public int getItemCount() {
            return samples.size();
        }
    }

    private void fetchRoute() {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(ORIGIN)
                .destination(DESTINATION)
                .alternatives(true)
                .build()
                .getRoute(new SimplifiedCallback() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Log.i(TAG, "Route response received");
                        currentRoute = response.body().routes().get(0);
                    }
                });
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
}
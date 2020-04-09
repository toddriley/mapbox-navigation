package com.example.mapboxrepro;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapBoxClient {
    private static final String TAG = "MapBoxClient";
    private static MapBoxClient instance = new MapBoxClient();

    private int routeRequestFailCount = 0;
    private DirectionsRoute currentRoute;
    private RouteRequest previousSuccessfulRequest = null;

    public static MapBoxClient getInstance() {
        return instance;
    }

    public void buildRoute(Context context,
                           @NonNull RouteRequest routeRequest,
                           Consumer<DirectionsRoute> successCallback) {
        buildRoute(context, routeRequest, successCallback, null);
    }

    public void buildRoute(Context context,
                           @NonNull RouteRequest routeRequest,
                           Consumer<DirectionsRoute> successCallback,
                           @Nullable Consumer<Void> failureCallback
    ) {
        if (routeRequest.equals(previousSuccessfulRequest)) {
            Log.v(TAG, "Requesting duplicate route, returning previous response.");
            successCallback.accept(currentRoute);
            return;
        }
        NavigationRoute.Builder routeBuilder = NavigationRoute.builder(context)
                .accessToken(Mapbox.getAccessToken())
                .origin(routeRequest.getOrigin());

        routeBuilder.destination(routeRequest.getDestination())
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        resetRouteRequestFailCount();
                        if (response.body() == null) {
                            Log.e(TAG,"No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG,"No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);
                        previousSuccessfulRequest = routeRequest;
                        successCallback.accept(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG,"Error: %s", throwable);
                        MapBoxClient.getInstance().incrementRouteRequestFailCount();
                        if (failureCallback != null) {
                            failureCallback.accept(null);
                        }
                    }
                });
    }

    public int incrementRouteRequestFailCount() {
        return ++routeRequestFailCount;
    }

    public void resetRouteRequestFailCount() {
        routeRequestFailCount = 0;
    }
}

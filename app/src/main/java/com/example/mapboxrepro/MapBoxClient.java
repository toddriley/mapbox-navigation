package com.example.mapboxrepro;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MapBoxClient {

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
            Timber.v("Requesting duplicate route, returning previous response.");
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
                        // You can get the generic HTTP info about the response
                        Timber.d("Response code: %s", response.code());
                        if (response.body() == null) {
                            Timber.e("No routes found, make sure you set the right user and access token.  Error code %s.  Error message %s", response.code(), response.message());
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Timber.e("No routes found.  Error code %s.  Error message %s", response.code(), response.message());
                            return;
                        }

                        currentRoute = response.body().routes().get(0);
                        previousSuccessfulRequest = routeRequest;
                        successCallback.accept(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Timber.e("Error: %s", throwable.getMessage());
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

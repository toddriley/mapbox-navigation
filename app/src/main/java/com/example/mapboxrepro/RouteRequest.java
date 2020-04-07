package com.example.mapboxrepro;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Point;

import java.io.Serializable;
import java.util.Objects;

public class RouteRequest implements Serializable {
    private Point origin, destination;

    private static final String ORIGIN_KEY = "origin";
    private static final String DESTINATION_KEY = "destination";

    RouteRequest(@NonNull Point origin, @NonNull Point destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public Point getOrigin() {
        return origin;
    }

    public Point getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteRequest that = (RouteRequest) o;
        return origin.equals(that.origin) &&
                destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination);
    }
}

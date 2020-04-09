package com.example.mapboxrepro;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeasurement;

import java.util.List;

public class MapDecorator {
    private static final String ENDPOINT_ICON_KEY = "endpoint_icon";

    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String STYLE_URI = "mapbox://styles/mapbox/navigation-guidance-day-v3";
    private static final String LAYER_ID = "LAYER_ID";

    @NonNull
    static Style.Builder getStyleBuilderWithStops(@NonNull Resources resources, @NonNull List<Feature> symbolLayerIconFeatureList) {
        return getStyleBuilder(resources)
                .withSource(new GeoJsonSource(SOURCE_ID,
                        FeatureCollection.fromFeatures(symbolLayerIconFeatureList))
                );
    }

    @NonNull
    static Style.Builder getStyleBuilder(@NonNull Resources resources) {
        Drawable endpointIcon = resources.getDrawable(R.drawable.ic_aap_flag_no_circle_black, null);

        return new Style.Builder().fromUri(STYLE_URI)
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
    }

    @NonNull
    static LatLngBounds getRouteBounds(@NonNull DirectionsRoute route) {
        LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
        double[] bounds = TurfMeasurement.bbox(lineString);
        return turfBboxToLatLngBounds(bounds);
    }

    @NonNull
    private static LatLngBounds turfBboxToLatLngBounds(@NonNull double[] bounds) {
        double minLongitude = bounds[0];
        double minLatitude = bounds[1];
        double maxLongitude = bounds[2];
        double maxLatitude = bounds[3];
        LatLng min = new LatLng(minLatitude, minLongitude);
        LatLng max = new LatLng(maxLatitude, maxLongitude);
        return new LatLngBounds.Builder()
                .include(min)
                .include(max)
                .build();
    }
}

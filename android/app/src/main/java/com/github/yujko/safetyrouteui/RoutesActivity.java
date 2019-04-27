/**
 * 1) Add street map
 * 2) add long lat and reflect it
 * 3) add a mock route
 * 4) add
 */




package com.github.yujko.safetyrouteui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.github.yujko.safetyrouteui.R;

public class RoutesActivity extends AppCompatActivity {

    ArcGISMap mMap;
    MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        // get latitude and longitude of the target from previous activity
        Intent intent = getIntent();
        Float lat = Float.parseFloat(intent.getStringExtra(LoginActivity.LATITUDE));
        Float lng = Float.parseFloat(intent.getStringExtra(LoginActivity.LONGITUDE));

        // render map
        mMap = new ArcGISMap(Basemap.Type.IMAGERY, lat, lng, 10);
        mMapView = findViewById(R.id.routesView);
        mMapView.setMap(mMap);
    }


}

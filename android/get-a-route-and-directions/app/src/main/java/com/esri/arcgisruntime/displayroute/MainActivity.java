package com.esri.arcgisruntime.displayroute;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.esri.arcgisruntime.geometry.SpatialReferences;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private GraphicsOverlay mGraphicsOverlay;
    private Point mStart;
    private Point mEnd;

    private void createGraphicsOverlay() {
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }

    private void setMapMarker(Point location, SimpleMarkerSymbol.Style style, int markerColor, int outlineColor) {
        float markerSize = 8.0f;
        float markerOutlineThickness = 2.0f;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(style, markerColor, markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, outlineColor, markerOutlineThickness));
        Graphic pointGraphic = new Graphic(location, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    private void setStartMarker(Point location) {
        mGraphicsOverlay.getGraphics().clear();
        setMapMarker(location, SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(226, 119, 40), Color.BLUE);
        mStart = location;
        mEnd = null;
    }

    private void setEndMarker(Point location) {
        setMapMarker(location, SimpleMarkerSymbol.Style.SQUARE, Color.rgb(40, 119, 226), Color.RED);
        mEnd = location;
        reloadMap();
        findRoute();
//        findBetterRoute();
    }

    // Determine how to handle a tap on the map.
    private void mapClicked(Point location) {
        if (mStart == null) {
            // Start is not set, set it to a tapped location
            setStartMarker(location);
        } else if (mEnd == null) {
            // End is not set, set it to the tapped location then find the route
            setEndMarker(location);
        } else {
            // Both locations are set; re-set the start to the tapped location
            setStartMarker(location);
        }
    }

    private void reloadMap() {
        Portal portal = new Portal("http://www.arcgis.com");
        PortalItem mapPortalItem = new PortalItem(portal, "cbef0414dd9d41d3bff10296ca74ea14");
        ArcGISMap map = new ArcGISMap(mapPortalItem);
        mMapView.setMap(map);
    }

    // Find a route from mStart point to mEnd point.
    private void findRoute() {
        String routeServiceURI = getResources().getString(R.string.routing_url);
        final RouteTask solveRouteTask = new RouteTask(getApplicationContext(), routeServiceURI);
        solveRouteTask.loadAsync();
        solveRouteTask.addDoneLoadingListener(() -> {
            if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {

                final ListenableFuture<RouteParameters> routeParamsFuture = solveRouteTask.createDefaultParametersAsync();
                routeParamsFuture.addDoneListener(() -> {
                    try {
                        RouteParameters routeParameters = routeParamsFuture.get();
                        List<Stop> stops = new ArrayList<>();
                        stops.add(new Stop(mStart));
                        stops.add(new Stop(mEnd));
                        routeParameters.setStops(stops);
                        final ListenableFuture<RouteResult> routeResultFuture = solveRouteTask.solveRouteAsync(routeParameters);
                        routeResultFuture.addDoneListener(() -> {
                            try {
                                RouteResult routeResult = routeResultFuture.get();
                                Route firstRoute = routeResult.getRoutes().get(0);

                                Polyline routePolyline = firstRoute.getRouteGeometry();
                                SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.GREEN, 4.0f);
                                Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                mGraphicsOverlay.getGraphics().add(routeGraphic);
                            } catch (InterruptedException | ExecutionException e) {
                                showError("Solve RouteTask failed " + e.getMessage());
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        showError("Cannot create RouteTask parameters " + e.getMessage());
                    }
                });

                final ListenableFuture<RouteParameters> betterRouteParamsFuture = solveRouteTask.createDefaultParametersAsync();
                betterRouteParamsFuture.addDoneListener(() -> {
                    try {
                        RouteParameters betterRouteParameters = betterRouteParamsFuture.get();
                        List<Stop> stops = new ArrayList<>();
                        stops.add(new Stop(mStart));
                        stops.add(new Stop(Point.createWithM(51.04,-113.1, 10)));
                        stops.add(new Stop(mEnd));
                        betterRouteParameters.setStops(stops);
                        final ListenableFuture<RouteResult> betterRouteResultFuture = solveRouteTask.solveRouteAsync(betterRouteParameters);
                        betterRouteParamsFuture.addDoneListener(() -> {
                            try {
                                RouteResult routeResult = betterRouteResultFuture.get();
                                Route firstRoute = routeResult.getRoutes().get(0);
                                Polyline routePolyline = firstRoute.getRouteGeometry();
                                SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 4.0f);
                                Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                mGraphicsOverlay.getGraphics().add(routeGraphic);
                            } catch (InterruptedException | ExecutionException e) {
                                showError("Solve RouteTask failed " + e.getMessage());
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        showError("Cannot create RouteTask parameters " + e.getMessage());
                    }
                });


            } else {
                showError("Unable to load RouteTask " + solveRouteTask.getLoadStatus().toString());
            }
        });
    }

    private void findBetterRoute() {
        String routeServiceURI = getResources().getString(R.string.routing_url);
        final RouteTask solveRouteTask = new RouteTask(getApplicationContext(), routeServiceURI);
        solveRouteTask.loadAsync();
        solveRouteTask.addDoneLoadingListener(() -> {
            if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {

                final ListenableFuture<RouteParameters> betterRouteParamsFuture = solveRouteTask.createDefaultParametersAsync();
                betterRouteParamsFuture.addDoneListener(() -> {
                    try {
                        RouteParameters betterRouteParameters = betterRouteParamsFuture.get();
                        List<Stop> stops = new ArrayList<>();
                        stops.add(new Stop(mStart));
                        stops.add(new Stop(Point.createWithM(51.04,-113.1, 10)));
                        stops.add(new Stop(mEnd));
                        betterRouteParameters.setStops(stops);
                        final ListenableFuture<RouteResult> betterRouteResultFuture = solveRouteTask.solveRouteAsync(betterRouteParameters);
                        betterRouteParamsFuture.addDoneListener(() -> {
                            try {
                                RouteResult routeResult = betterRouteResultFuture.get();
                                Route firstRoute = routeResult.getRoutes().get(0);
                                Polyline routePolyline = firstRoute.getRouteGeometry();
                                SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 4.0f);
                                Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                mGraphicsOverlay.getGraphics().add(routeGraphic);
                            } catch (InterruptedException | ExecutionException e) {
                                showError("Solve RouteTask failed " + e.getMessage());
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        showError("Cannot create RouteTask parameters " + e.getMessage());
                    }
                });


            } else {
                showError("Unable to load RouteTask " + solveRouteTask.getLoadStatus().toString());
            }
        });
    }

    private void showError(String message) {
        Log.d("FindRoute", message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type basemapType = Basemap.Type.STREETS_VECTOR;
            double latitude = 51.04;
            double longitude = -114.08;
            int levelOfDetail = 10;
            Portal portal = new Portal("http://www.arcgis.com");
            PortalItem mapPortalItem = new PortalItem(portal, "65803bd91e214dbeb8cd7a1cdc3f1438");
            ArcGISMap map = new ArcGISMap(mapPortalItem);
//            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            mMapView.setMap(map);

            mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
                @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                    android.graphics.Point screenPoint = new android.graphics.Point(
                            Math.round(e.getX()),
                            Math.round(e.getY()));
                    Point mapPoint = mMapView.screenToLocation(screenPoint);
                    mapClicked(mapPoint);
                    return super.onSingleTapConfirmed(e);
                }
            });
        }
    }

    private void setupOAuthManager() {
        String clientId = getResources().getString(R.string.client_id);
        String redirectUri = getResources().getString(R.string.redirect_uri);

        try {
            OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com", clientId, redirectUri);
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            showError(e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        setupOAuthManager();
        setupMap();
        createGraphicsOverlay();
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }
}

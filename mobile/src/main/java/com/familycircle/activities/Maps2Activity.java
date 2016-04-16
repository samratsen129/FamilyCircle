package com.familycircle.activities;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.lib.utils.Logger;
import com.familycircle.manager.PubSubManager;
import com.familycircle.manager.TeamManager;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.model.UserObject;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONObject;

public class Maps2Activity extends FragmentActivity implements PubSubManager.OnPubNubMessage, OnMapReadyCallback {

    private MapView mapView;
    private String userId;
    double lat=33.743992, lon=-84.388866;
    double userlat=33.743992, userlon=-84.388866;
    boolean isFirstPoint = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        // Get current users location
        ContactModel contactModel = ContactsStaticDataModel.getLogInUser();
        String messageValue = contactModel.location;
        if (messageValue!=null) {
            String[] messages = messageValue.split(",");
            if (messages.length > 1) {

                for (String msg : messages) {
                    if (msg.contains("lat")) {
                        userlat = Double.parseDouble(msg.split(":")[1]);

                    }
                    if (msg.contains("long")) {
                        userlon = Double.parseDouble(msg.split(":")[1]);
                    }
                }
            }
        }

        mapView.getMapAsync(Maps2Activity.this);

        userId = getIntent().getStringExtra("TAG_ID");
        Logger.d("Map for User " + userId);
        TeamManager.getInstance().sendCommandMessage("enable_location", userId);

    }

    private double getDistance(){

        try {
            Location selected_location = new Location("user");
            selected_location.setLatitude(userlat);
            selected_location.setLongitude(userlon);
            Location near_locations = new Location(userId);
            near_locations.setLatitude(lat);
            near_locations.setLongitude(lon);

            return selected_location.distanceTo(near_locations);

        } catch(Exception e){
            Logger.e("Distance calculation error", e);
        }

        return 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        PubSubManager.getInstance().addListener(this);
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        PubSubManager.getInstance().removeListener(this);
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
    public void onPubNubMessage(String channel, Object message, JSONObject ignore) {
        Logger.d("Map Activity received " + message);
        try {

            JSONObject jsonObject = new JSONObject(message.toString());
            UserObject userObject = LoginRequest.getUserObject();
            final String from = jsonObject.getString("from");
            String type = jsonObject.getString("type");

            if (type.equalsIgnoreCase("location")) {
                String messageValue = jsonObject.getString("value");
                String[] messages = messageValue.split(",");

                for (String msg:messages){
                    if (msg.contains("lat")){
                        lat = Double.parseDouble(msg.split(":")[1]);
                    }
                    if (msg.contains("long")){
                        lon = Double.parseDouble(msg.split(":")[1]);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapView.getMapAsync(Maps2Activity.this);
                    }
                });

            }

        } catch (Exception e){
            Logger.e("Error while parsing map payload", e);
        }
    }


    @Override
    public void onConnect(String channel, Object message) {

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Set map style
        mapboxMap.setStyleUrl(Style.MAPBOX_STREETS);

        // Set the camera's starting position
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lon)) // set the camera's center position
                .zoom(12)  // set the camera's zoom level
                .tilt(20)  // set the camera's tilt
                .build();

        // Move the camera to that position
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (!isFirstPoint){
            mapboxMap.addMarker(new MarkerOptions().title(userId).position(new LatLng(lat, lon)));
            Toast.makeText(getApplicationContext(), "Your distance from " + userId + " =>" + getDistance(), Toast.LENGTH_LONG).show();
        } else {
            mapboxMap.addMarker(new MarkerOptions().title("You").position(new LatLng(lat, lon)));
        }

        isFirstPoint = false;
    }
}

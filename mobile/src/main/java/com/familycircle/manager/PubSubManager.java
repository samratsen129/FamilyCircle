package com.familycircle.manager;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.familycircle.TeamApp;
import com.familycircle.lib.receiver.INetworkStatusChange;
import com.familycircle.lib.receiver.NetworkChangeReceiver;
import com.familycircle.lib.utils.Logger;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.NotificationMgr;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateStream;
import com.familycircle.utils.network.M2XCreateStreamValue;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.model.UserObject;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by samratsen on 4/9/16.
 */
public class PubSubManager implements INetworkStatusChange, GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static String TAG = "PubSubManager";
    private static PubSubManager _instance = new PubSubManager();
    private HashMap<String, Integer> subscriberList = new HashMap<String, Integer>();
    private List<OnPubNubMessage> callbacks = new ArrayList<OnPubNubMessage>();
    public boolean _isNetworkConnected = true;
    private Pubnub mPubNub;
    private GoogleApiClient mGoogleApiClient;
    private LatLng mLatLng;
    public static long locationsampleInterval = 20000 ;//10000;
    public static long locationfasterInterval = 10000;//5000;
    public boolean isStarted = false;

    @Override
    public void onNetworkStateChange(boolean isNetworkConnected, Constants.NetworkType type) {
        if (isNetworkConnected && _isNetworkConnected!=isNetworkConnected){
            ContactModel contactModel = ContactsStaticDataModel.getLogInUser();
            if (STATUS!=STATE.CONNECTED) {
                Set<String> keySet = subscriberList.keySet();
                for (String key:keySet){
                    try {
                        mPubNub.subscribe(key, mSubscribeReceiver);
                    } catch (Exception e){
                        Logger.e("subscribe error with pubnub ", e);
                    }
                }
            }
        }
        _isNetworkConnected = isNetworkConnected;

    }

    public void addListener(OnPubNubMessage onPubNubMessage){
        if (find(onPubNubMessage)) return;
        callbacks.add(onPubNubMessage);
    }

    public void removeListener(OnPubNubMessage onPubNubMessage){
        for (OnPubNubMessage onPubNubMessage1:callbacks){
            if (onPubNubMessage1==onPubNubMessage) {
                callbacks.remove(onPubNubMessage1);
                return;
            }

        }
    }

    public boolean find(OnPubNubMessage onPubNubMessage){
        for (OnPubNubMessage onPubNubMessage1:callbacks){
            if (onPubNubMessage1==onPubNubMessage) return true;

        }

        return false;
    }

    public enum STATE {
        CONNECTED, DISCONNECTED
    }

    public STATE STATUS = STATE.DISCONNECTED;

    private PubSubManager (){
        this.mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        mPubNub.setMaxRetries(5);
        subscriberList = new HashMap<String, Integer>();
        NetworkChangeReceiver.addForNetworkStateListener(this);
    }

    public static PubSubManager getInstance(){
        return _instance;
    }

    public boolean subscribe(String channel, String deviceId) {
        try {

            if (subscriberList.containsKey(channel) && (STATUS != STATE.DISCONNECTED)) return true;

            mPubNub.setUUID(deviceId);
            mPubNub.subscribe(channel, mSubscribeReceiver);
            //mPubNub.setHeartbeat(200);

        } catch (PubnubException e){
            Logger.e("subscribe error with pubnub ", e);
            STATUS = STATE.DISCONNECTED;
            return false;
        }

        return true;
    }

    Callback mSubscribeReceiver = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE :  connectCallback " + channel + "," + message);
            subscriberList.put(channel,1);
            for (OnPubNubMessage onPubNubMessage1:callbacks){
                onPubNubMessage1.onConnect(channel, message);
            }
        }

        @Override
        public void successCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE :  Message Received: " + message.toString());
            try {
                JSONObject jsonObject = new JSONObject(message.toString());
                UserObject userObject = LoginRequest.getUserObject();
                String from = jsonObject.getString("from");
                if (userObject != null && from != null) {
                    if (userObject.email.equalsIgnoreCase(from)){
                        return;
                    }
                }
                String type = jsonObject.getString("type");

                if (type.equalsIgnoreCase("panic")) {
                    String messageValue = jsonObject.getString("value");


                    NotificationMgr notificationMgr = new NotificationMgr();
                    notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, "Alert", from, messageValue);
                }

                if (type.equalsIgnoreCase("heardbeat")
                        || type.equalsIgnoreCase("heardrate")) {
                    String messageValue = jsonObject.getString("value");


                    NotificationMgr notificationMgr = new NotificationMgr();
                    notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, "Alert", from, "An unusual heartrate is being reported. Heart Rate - " + messageValue);
                }


                if (type.equalsIgnoreCase("distance_stream")
                        || type.equalsIgnoreCase("door distance")) {
                    String messageValue = jsonObject.getString("value");


                    NotificationMgr notificationMgr = new NotificationMgr();
                    notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, "Alert", from, "Someone is near your door. Distance - " + messageValue);
                }

            } catch (Exception e){
                Logger.e("Exception during parsing message", e);
            }

            for (OnPubNubMessage onPubNubMessage1:callbacks){
                onPubNubMessage1.onPubNubMessage(channel, message);
            }
            /*JSONObject jsonMessage = (JSONObject) message;
            try {
                double mLat = jsonMessage.getDouble("lat");
                double mLng = jsonMessage.getDouble("lng");
                //mLatLng = new LatLng(mLat, mLng);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }*/

        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE : DISCONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
            STATUS = STATE.DISCONNECTED;

        }

        public void reconnectCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE : RECONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            super.errorCallback(channel, error);
            Logger.e("SUBSCRIBE : ERROR on channel:" + channel
                    + " : " + error.getErrorString() + " : "
                    + error.errorObject);
            STATUS = STATE.DISCONNECTED;

        }
    };

    public void startSharingLocation() {
        if (isStarted) return;
        Log.d(TAG, "Starting Location Updates");

        if (mGoogleApiClient==null) {
            mGoogleApiClient = new GoogleApiClient.Builder(TeamApp.getContext())
                    .addConnectionCallbacks(this).addApi(LocationServices.API)
                    .build();
        }
        isStarted = true;
        mGoogleApiClient.connect();
    }

    public void stopSharingLocation() {
        if (!isStarted) return;
        Log.d(TAG, "Stop Location Updates & Disconect to Google API");
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        isStarted = false;
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    // =============================================================================================
    // Google Location API CallBacks
    // =============================================================================================

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected to Google API for Location Management");
        //if (mRequestingLocationUpdates) {
            LocationRequest mLocationRequest = createLocationRequest();
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        //}
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location Detected");
        mLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        UserObject userObject = LoginRequest.getUserObject();
        String messageLabel = "lat: " + location.getLatitude() + ", " + "long: " + location.getLongitude() + ", alt: " + location.getAltitude();

        ContactModel userContact = ContactsStaticDataModel.getLogInUser();
        userContact.location = messageLabel;

        // Send to M2X
        M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue(null, userObject.m2x_id, "location", "alphanumeric", messageLabel);
        m2XCreateStream.exec();

        // Broadcast information on PubNub Channel
        //PubNubManager.broadcastLocation(mPubnub, channelName, location.getLatitude(),
        //       location.getLongitude(), location.getAltitude());

        // Update Map

    }

    private LocationRequest createLocationRequest() {
        Log.d(TAG, "Building request");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(locationsampleInterval);
        mLocationRequest.setFastestInterval(locationfasterInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    // =============================================================================================
    // Button CallBacks
    // =============================================================================================


    public interface OnPubNubMessage {
        public void onPubNubMessage(String channel, Object message);
        public void onConnect(String channel, Object message);
    }
}

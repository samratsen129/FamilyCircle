package com.familycircle.lib.utils.pubnub;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.familycircle.lib.AppRTCClient;
import com.familycircle.lib.receiver.INetworkStatusChange;
import com.familycircle.lib.receiver.NetworkChangeReceiver;
import com.familycircle.lib.utils.AudioPlayer;
import com.familycircle.lib.utils.Logger;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.sdk.BaseApp;
import com.familycircle.sdk.ChannelManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.models.CallModel;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.Device;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.halfbit.tinybus.Bus;
import de.halfbit.tinybus.TinyBus;

public class PnRTCManager  implements INetworkStatusChange {
    private Pubnub mPubNub;
    private PnRTCReceiver mSubscribeReceiver;
    private String id; // current users id
    private ContactModel fromUser;
    private List<AppRTCClient.WebSocketChannelEvents> mListeners;

    private static PnRTCManager _pnRTCManager = new PnRTCManager();
    private Bus bus;
    private AppRTCClient.SignalingParameters params;
    private boolean initiator = false;
    private String sessionId = UUID.randomUUID().toString();
    private boolean _isNetworkConnected = false;

    @Override
    public void onNetworkStateChange(boolean isNetworkConnected, Constants.NetworkType type) {
        if (isNetworkConnected && _isNetworkConnected!=isNetworkConnected){
            if (STATUS!=STATE.CONNECTED) {
                subscribe(Constants.STDBY_CHANNEL);
            }
        }
        _isNetworkConnected = isNetworkConnected;
    }


    public enum STATE {
        CONNECTED, DISCONNECTED, REGISTERED
    }

    public STATE STATUS;


    private PnRTCManager(){
        mListeners = new ArrayList<AppRTCClient.WebSocketChannelEvents>();
        STATUS = STATE.DISCONNECTED;
        bus = TinyBus.from(BaseApp.getContext());
        NetworkChangeReceiver.addForNetworkStateListener(this);
    }

    public Bus getBus(){
        return bus;
    }


    public void addListener(AppRTCClient.WebSocketChannelEvents event){
        if (event==null) return;
        for (AppRTCClient.WebSocketChannelEvents _event: mListeners){
            if (_event!=null && _event == event){
                return;
            }
        }
        mListeners.add(event);
    }

    public void removeListener(AppRTCClient.WebSocketChannelEvents event){
        if (event==null|| mListeners ==null) return;
        Iterator<AppRTCClient.WebSocketChannelEvents> itr = mListeners.iterator();
        while(itr.hasNext()){
            AppRTCClient.WebSocketChannelEvents _event = itr.next();
            if (_event!=null && _event == event){
                itr.remove();
            }
        }
    }

    public ContactModel getUser(){
        return fromUser;
    }

    public static PnRTCManager getInstance(){
        return _pnRTCManager;
    }

    public synchronized void disconnect() {
        if (STATUS== STATE.DISCONNECTED) {
            Logger.d("Already Dis-Connected");
            return;
        }
        STATUS= STATE.DISCONNECTED;
        if (mPubNub != null) {
            mPubNub.unsubscribeAll();
        }

    }


    /**
     * @param toID The id or "number" that you wish to transmit a message to.
     * @param packet The JSON data to be transmitted
     */
    public boolean transmitMessage(String toID, JSONObject packet){
        if (STATUS == STATE.DISCONNECTED){ // Not logged in. Put an error in the debug cb.
            Logger.e("Cannot transmit before calling Client.connect");
            return false;
        }

        try {
            long t = (new Date()).getTime();

            if (packet.isNull("timestamp")){
                packet.put("timestamp", t);
            }

            if (packet.isNull("id")){
                packet.put("id", UUID.randomUUID().toString());
            }

            JSONObject message = new JSONObject();
            message.put(PnRTCMessage.JSON_PACKET, packet);
            message.put(PnRTCMessage.JSON_ID, ""); //Todo: session id, unused in js SDK?
            message.put(PnRTCMessage.JSON_NUMBER, toID);

            Logger.d("transmitMessage ==>" + message);

            this.mPubNub.publish(Constants.STDBY_CHANNEL, message, new Callback() {  // Todo: reconsider callback.
                @Override
                public void successCallback(String channel, Object message, String timetoken) {
                    Logger.d("transmitMessage publish success" + message);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Logger.e("transmitMessage Error during publish" + error.errorObject);
                }
            });

        } catch (Exception e){
            Logger.e("Error while sending message to pubnub (publish)", e);
            return false;
        }

        return true;
    }

    public synchronized void connect(final ContactModel fromUser){
        try {
            this.mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
            mPubNub.setMaxRetries(5);
            this.mPubNub.setUUID(fromUser.getIdTag());
            this.fromUser = fromUser;
            this.id = fromUser.getIdTag();
            if (STATUS == STATE.CONNECTED || STATUS == STATE.REGISTERED) {
                Logger.d("Already Connected");
                if (mListeners != null) {
                    for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                        if (_event != null) {
                            _event.onWebSocketOpen();
                        }
                    }
                }
                return;
            } else {
                mSubscribeReceiver = new PnRTCReceiver();
                subscribe(Constants.STDBY_CHANNEL);
            }
        } catch (Exception e){
            Logger.e("connect error with pubnub ", e);
            STATUS = STATE.DISCONNECTED;
            if (mListeners != null) {
                for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                    if (_event != null) {
                        _event.onWebSocketError("unable to connect to pubnub");
                    }
                }
            }
        }
    }

    private void subscribe(String channel) {
        try {
            mPubNub.subscribe(channel, this.mSubscribeReceiver);
            mPubNub.presence(channel, this.mPresenceReceiver);
            //mPubNub.setHeartbeat(200);

        } catch (PubnubException e){
            Logger.e("subscribe error with pubnub ", e);
            STATUS = STATE.DISCONNECTED;
            if (mListeners != null) {
                for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                    if (_event != null) {
                        _event.onWebSocketError("subscribe error with pubnub ");
                    }
                }
            }
        }
    }


    private class PnRTCReceiver extends Callback {

        @Override
        public void connectCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE :  connectCallback " + channel + "," + message);
            STATUS = STATE.CONNECTED;
            STATUS = STATE.REGISTERED;
            sessionId = UUID.randomUUID().toString();
            // Init dummey parameters start
            // No ICE values initially
            String sessionAuthToken = UUID.randomUUID().toString();
            LinkedList<IceCandidate> iceCandidates = null;
            LinkedList<PeerConnection.IceServer> iceServers = null;
            SessionDescription offerSdp = null;

            // All Media constraints missing
            MediaConstraints pcConstraints = new MediaConstraints();
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

            MediaConstraints videoConstraints = new MediaConstraints();
            MediaConstraints audioConstraints = new MediaConstraints();

            params = new AppRTCClient.SignalingParameters(
                    iceServers, initiator,
                    pcConstraints, videoConstraints, audioConstraints,
                    sessionAuthToken, "wss://teampresence.mpsvcs.com/"+sessionAuthToken, null,
                    offerSdp, iceCandidates);
            // Init dummey parameters end

            for (AppRTCClient.WebSocketChannelEvents _event: mListeners){
                if (_event!=null){
                    _event.onWebSocketOpen();
                }
            }

            getAllUsers();
        }

        @Override
        public void successCallback(String channel, Object message) {
            if (!(message instanceof JSONObject)) return; // Ignore if not valid JSON.
            JSONObject jsonMessage = (JSONObject) message;
            //Logger.d("SUBSCRIBE : Received JSON " + jsonMessage);

            try {
                String peerId     = jsonMessage.getString(PnRTCMessage.JSON_NUMBER);

                // No Need to handle if this is from same device
                if (!peerId.equalsIgnoreCase(id)){
                    Logger.d("SUBSCRIBE : Received JSON but not for thsi peer " + peerId+"<>"+id);
                    return;
                }
                JSONObject packet = jsonMessage.getJSONObject(PnRTCMessage.JSON_PACKET);

                String type = null;
                try {
                    type = packet.optString("type");
                } catch (Exception e){

                }

                if (type!=null && (type.equalsIgnoreCase("error") ||
                        type.equalsIgnoreCase("presence")||
                        type.equalsIgnoreCase("call"))){
                    try {
                        handleMessage(type, packet);
                    }  catch (Exception e){
                        Logger.e("Error in json parsing - handleMessage", e);
                    }
                    //return;
                }

                for (AppRTCClient.WebSocketChannelEvents _event: mListeners){
                    if (_event!=null){
                        _event.onWebSocketMessage(packet.toString());
                    }
                }

            } catch (JSONException e){
                Logger.e("SUBSCRIBE : Received JSON Parse exception in successcallback", e);
                if (mListeners != null) {
                    for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                        if (_event != null) {
                            _event.onWebSocketError("unable to parse received message ");
                        }
                    }
                }
            }
        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Logger.d("SUBSCRIBE : DISCONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
            STATUS = STATE.DISCONNECTED;
            if (mListeners != null) {
                for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                    if (_event != null) {
                        _event.onWebSocketError("control channel disconnected ");
                    }
                }
            }
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
            if (mListeners != null) {
                for (AppRTCClient.WebSocketChannelEvents _event : mListeners) {
                    if (_event != null) {
                        _event.onWebSocketError("control channel error ");
                    }
                }
            }
        }

    }

    private void handleMessage(final String type, final JSONObject json) throws Exception {
        if (type.equals("error")) {
            String errorCode = json.getString("code");
            String errorMsg = json.getString("message");
            Logger.e("Error (" + errorCode + ") " + errorMsg);
            //Toast.makeText(BaseApp.getContext(), "Error (" + errorCode + ") " + errorMsg, Toast.LENGTH_LONG);

        } else if (type.equals("presence")) {
            ContactsStaticDataModel.clearContacts();
            JSONArray usersArray = json.getJSONArray("users");
            if (usersArray!=null && usersArray.length()>0){

                for (int i=0; i<usersArray.length();i++){
                    ContactModel contact = new ContactModel();

                    JSONObject userObject = usersArray.getJSONObject(i);
                    if (!userObject.isNull("status")){
                        contact.setStatus(userObject.getString("status"));
                    }
                    JSONArray userIdArray = userObject.getJSONArray("id");
                    if (userIdArray!=null && userIdArray.length()>0){
                        for (int ii=0; ii<userIdArray.length();ii++) {
                            String userId = (String)userIdArray.get(ii);
                            if (ii==0){
                                contact.setIdTag(userId.trim());
                            }
                        }

                        JSONArray userDevicesArray =  userObject.getJSONArray("devices");
                        if (userDevicesArray!=null && userDevicesArray.length()>0){
                            List<Device> devices = new ArrayList<Device>(userDevicesArray.length());
                            for (int ii=0; ii<userDevicesArray.length();ii++) {
                                Device device = new Device();
                                JSONObject userDeviceObject = userDevicesArray.getJSONObject(ii);
                                device.setId(userDeviceObject.getString("id"));
                                device.setDevice(userDeviceObject.getString("device"));
                                device.setLocation(userDeviceObject.getString("location"));
                                device.setStatusMessage(userDeviceObject.getString("statusMessage"));
                                devices.add(device);
                            }
                            contact.setDevices(devices);
                        }

                        ContactsStaticDataModel.addContact(contact);
                    }

                }

            }
            //bus.post(new BusEvents.ProcessPresenceEvent());

        }
    }

    private void handleIncomingCall(String fromId, String callTag){
        //if (Foreground.instance.isBackground()){
        Intent mainIntent = new Intent();//BaseApp.getContext(), IncomingCallActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mainIntent.setAction(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_HOME);
        mainIntent.putExtra("CALL_TYPE", "join");
        mainIntent.putExtra("CALLEE_ID", fromId);
        mainIntent.putExtra("CALL_TAG", callTag);
        mainIntent.putExtra("incomingCallInfo", "WebSocketManager");
        mainIntent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        BaseApp.getContext().startActivity(mainIntent);

        //}
        // if ((activeCalls==null || activeCalls.isEmpty())
        // && callHoldItem==null) {
        PrefManagerBase prefManager = new PrefManagerBase();
        if (prefManager.getIsPlayRingtoneSetting()) {
            String ringtoneName = prefManager.getRingtoneNameSetting();

            if (ringtoneName == null) {
                ringtoneName = "perfect_ringtone.mp3";
            }

            AudioPlayer audioPlayer = AudioPlayer.getInstance();
            audioPlayer.playAudio(ringtoneName, null);
        }
        if (prefManager.getVibrateWhenRingingSetting()) {
            AudioPlayer.getInstance().setVibration();
        }
        // }

    }

    public void onRejectIncomingCall(final ContactModel event) {
        try {
            String jsonMsg = "{\"type\":\"call\", \"to\":[\"" + event.getIdTag() + "\"], \"from\":\"" + fromUser.getIdTag() + "\", \"mode\":\"video\", \"status\":\"cancel\", \"clientTag\":" + event.getIdTag() + "}";
            transmitMessage(event.getIdTag(), new JSONObject(jsonMsg));
        } catch (Exception e){

        }


    }

/*    public void onCallCancel(final ContactModel event) {
        try {
            AudioPlayer audioPlayer = AudioPlayer.getInstance();
            audioPlayer.releasePlayer();

        } catch (Exception e){
            Log.d(TAG, "Issue while closing media player",e);
        }
                try {
                    String jsonMsg = "{\"type\":\"call\", \"to\":[\"" + event.getIdTag() + "\"], \"from\":\"" + fromUser.getIdTag() + "\", \"mode\":\"video\", \"status\":\"cancel\", \"clientTag\":" + event.getIdTag() + "}";
                    sendMessage(new JSONObject(jsonMsg));
                } catch (Exception e){

                }

    }*/

    public void cancelCall(CallModel call){
        try {
            ContactModel fromContact = call.getFromContact();
            ContactModel toContact = call.getToContact();

            String jsonMsg = "{\"type\":\"call\", \"to\":[\"" + toContact.getIdTag() + "\"], \"from\":\"" + fromContact.getIdTag() + "\", \"mode\":\"video\", \"status\":\"cancel\", \"clientTag\":" + call.getCallTagId() + "}";
            transmitMessage(toContact.getIdTag(), new JSONObject(jsonMsg));
        } catch (Exception e){
            Logger.e("Cancel call exception:", e);
        }
    }

    public boolean getAllUsers(){
        try {

            mPubNub.hereNow(Constants.STDBY_CHANNEL, new Callback() {
                public void successCallback(String channel, Object response) {
                    Logger.d("GET_ALLUSERS on channel:" + channel + "=>" + response.toString());
                    if (response!=null && !response.toString().isEmpty()){
                        JSONObject pnPresenceAllUsers = (JSONObject)response;
                        ContactsStaticDataModel.clearContacts();
                        try {
                            JSONArray uuidArray = pnPresenceAllUsers.getJSONArray("uuids");
                            if (uuidArray!=null){
                                for (int i =0; i < uuidArray.length();i++){
                                    String uuid = uuidArray.getString(i);
                                    ContactModel contactModel = new ContactModel();
                                    contactModel.setEmail(uuid);
                                    contactModel.setFirstName(uuid);
                                    contactModel.setStatus("Online");
                                    contactModel.setIdTag(uuid);
                                    ContactsStaticDataModel.addContact(contactModel);
                                }
                            }
                        } catch (JSONException jex){
                            Logger.e("GET_ALLUSERS json parse exception:", jex);
                        }

                        ChannelManager.getInstance().getControlChannelEvent().onControlEvent(Constants.EventReturnState.GET_USERS_SUCCESS, null, "Users Received", null);
                    }
                }
                public void errorCallback(String channel, PubnubError error) {
                    Logger.e("GET_ALLUSERS on channel:" + channel + "=>" + error.toString());
                    ChannelManager.getInstance().getControlChannelEvent().onControlEvent(Constants.EventReturnState.GET_USERS_ERROR, null, "Unable to get users", null);
                }
            });

        } catch (Exception e){
            Logger.e("GET_ALLUSERS on channel exception" , e);
            ChannelManager.getInstance().getControlChannelEvent().onControlEvent(Constants.EventReturnState.GET_USERS_ERROR, null, "Unable to get users", null);
            return false;
        }

        return true;
    }

    public AppRTCClient.SignalingParameters getSignallingParameters(){
        return params;
    }

    private Callback mPresenceReceiver = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            Logger.d("PRESENCE CONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());

        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Logger.e("PRESENCE DISCONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            Logger.d("PRESENCE RECONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void successCallback(String channel, Object message) {
            Logger.d(channel + " PRESENCE SUCCESS on channel: " + channel
                    + message.getClass() + " : " + message.toString());

            try {
                JSONObject presencePnObject = (JSONObject)message;
                String action = presencePnObject.optString("action");

                if (action!=null && !action.isEmpty()){
                    if (action.equalsIgnoreCase("join")){
                        final String uuid = presencePnObject.getString("uuid");
                        ContactModel contactModel = new ContactModel();
                        contactModel.setIdTag(uuid);
                        contactModel.setEmail(uuid);
                        contactModel.setFirstName(uuid);
                        contactModel.setStatus("Online");
                        ContactsStaticDataModel.addContact(contactModel);
                    } else {
                        final String uuid = presencePnObject.getString("uuid");
                        ContactModel contactModel = new ContactModel();
                        contactModel.setIdTag(uuid);
                        contactModel.setEmail(uuid);
                        contactModel.setFirstName(uuid);
                        contactModel.setStatus("Offline");
                        ContactsStaticDataModel.addContact(contactModel);
                    }
                }

            } catch (Exception e){
                Logger.e("PRESENCE ERROR in success callback, json parsing ", e);
            }
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            Logger.e("PRESENCE ERROR on channel " + channel
                    + " : " + error.toString());
        }
    };
}

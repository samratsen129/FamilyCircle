package com.familycircle.sdk;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.familycircle.lib.AppRTCClient;
import com.familycircle.lib.receiver.INetworkStatusChange;
import com.familycircle.lib.receiver.NetworkChangeReceiver;
import com.familycircle.lib.utils.Logger;
import com.familycircle.lib.utils.MessagingClient;
import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.sdk.models.ChannelConfig;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 5/28/15.
 */
public final class ChannelManager implements AppRTCClient.WebSocketChannelEvents, INetworkStatusChange, IChannelManagerClient {
    private String TAG = "ChannelManager";
    private static ChannelManager _instance;

    public boolean isInitiated() {
        return _isInitiated;
    }

    public boolean isConnected() {
        return _isConnected;
    }

    private ChannelConfig _channelConfig;
    private int _connectionRetries=0;
    private IControlChannelEvent _controlChannelEvent;
    private IAVCallChannelEvent _webrtcChannelEvent;
    private boolean _isInitiated=false;
    private boolean _isConnected=false;
    private boolean _shutdownRequested=false;
    private boolean _isNetworkConnected=false;//keeps a track if network is connected or not in device
    private MessagingClient messagingClient;

    private ChannelManager(){

        PnRTCManager.getInstance().addListener(this);
        messagingClient = new MessagingClient();
        if (messagingClient!=null){
            Logger.d("messagingClient initialized");
        }
        NetworkChangeReceiver.addForNetworkStateListener(this);

    }

    public static ChannelManager getInstance(){

        if (_instance==null){
            _instance = new ChannelManager();
        }
        return _instance;
    }

    public boolean startChannel(ChannelConfig channelConfig, IControlChannelEvent eventListener){

        if ((eventListener==null) || (channelConfig==null)){
            if (eventListener!=null){
                eventListener.onControlEvent(Constants.EventReturnState.CONFIG_SETTINGS_ERROR, null, "Control Channel config cannot be null", null);
            }
            return false;
        }

        _isInitiated = true;
        _controlChannelEvent = eventListener;
        _channelConfig = channelConfig;
        _connectionRetries = 0;

        eventListener.onControlEvent(Constants.EventReturnState.CONFIG_START, null, "Control Channel config started", null);

        return true;
    }

    public IMessagingChannelClient registerMessagingClient(IMessagingChannelEvent messagingChannelEventListener){
        Logger.d("registerMessagingClient");
        if(!_isInitiated) return null;
        messagingClient.addMessagingEventListener(messagingChannelEventListener);
        Logger.d("added listener");
        if (messagingClient==null){
            Logger.e("registerMessagingClient returning null");
        } else {
            Logger.d("registerMessagingClient returning messagingClient");
        }
        return messagingClient;
    }

    public void unregisterMessagingClient(IMessagingChannelEvent messagingChannelEventListener){
        Logger.d("unregisterMessagingClient messagingClient");
        if(!_isInitiated) return;
        messagingClient.removeMessagingEventListener(messagingChannelEventListener);
    }

    public void removeControlChannelListener(AppRTCClient.WebSocketChannelEvents eventListener){
        PnRTCManager.getInstance().removeListener(eventListener);

    }

    public IControlChannelEvent getControlChannelEvent(){
        return _controlChannelEvent;
    }

    public void connectControlChannel(ContactModel contactModel){
        //if(!_isInitiated) return;
        if (_controlChannelEvent!=null && !_isInitiated){
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CONFIG_SETTINGS_ERROR, null, "Control Channel not set up", null);
        }

        if ((contactModel==null || contactModel.getIdTag()==null) && _controlChannelEvent!=null){
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.LOGIN_REQUIRED, null, "Login required", null);
            return;
        }

        _isConnected = false;

        ContactsStaticDataModel.setLogInUser(contactModel);
        Logger.d("connectControlChannel " + contactModel.getIdTag());
        PnRTCManager.getInstance().connect(contactModel);
        if (_shutdownRequested){
            _connectionRetries=0;
        }
        _shutdownRequested=false;
    }

    public void logOff(){
        _isConnected = false;
        _connectionRetries=0;
        PnRTCManager.getInstance().disconnect();

    }

    public void removeControlChannel(ContactModel contactModel){
        if(!_isInitiated) return;

        PnRTCManager.getInstance().disconnect();
        _shutdownRequested=true;
    }

    public void requestAllUsers(){
        if(!_isInitiated || !_isConnected) return;
        ContactModel loginUser = ContactsStaticDataModel.getLogInUser();

        try {
            PnRTCManager.getInstance().getAllUsers();

        } catch (Exception e) {
            e.printStackTrace();
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.GET_USERS_ERROR, null, "Could not retrieve users " + e.toString(), null);
        }
    }

    public void requestCreateNewUser(ContactModel contactModel){
        //if(!_isInitiated || !_isConnected) return;
        try {
            final String urlString = BaseApp.getUrl() + "create";
            Logger.d("requestCreateNewUser "+urlString);
            AsyncHttpClient client = new AsyncHttpClient();
            //RequestParams params = new RequestParams();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("email", contactModel.getIdTag());
            jsonObject.put("firstName", contactModel.getFirstName());
            jsonObject.put("lastName", contactModel.getLastName());
            jsonObject.put("mobileNumber", contactModel.getPhoneNumber());
            jsonObject.put("imageUrl", "http://cdn.flaticon.com/png/256/37943.png");
            jsonObject.put("iconUrl", "http://cdn.flaticon.com/png/256/37943.png");

            //String jsonStr = "{\"email\":\""+contactModel.getIdTag()+"\",\"firstName\":\""+contactModel.getFirstName()+"\",\"lastName\":\""+contactModel.getLastName()+"\",\"mobileNumber\":\""+contactModel.getPhoneNumber()+"\",\"imageUrl\":\"http://cdn.flaticon.com/png/256/37943.png\",\"iconUrl\":\"http://cdn.flaticon.com/png/256/37943.png\"}";
            //JSONObject jsonParams = new JSONObject();
            //jsonParams.put("user", jsonObject);
            String jsonStr = "user="+jsonObject.toString();
            Logger.d("requestCreateNewUser " + jsonStr);
            //params.put("user", jsonStr);
            //client.post(BaseApp.getContext(), urlString, params, asyncHttpResponseHandler);
            client.post(BaseApp.getContext(), urlString, new StringEntity(jsonStr), "application/json",
                    asyncHttpResponseHandler);


        } catch (Exception e) {
            Logger.e("Error in asynchttpresponse", e);
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CREATE_USER_ERROR, null, null, "User Could not be created, please check your network connection. " + e.toString());
        }
    }

    private final JsonHttpResponseHandler asyncHttpResponseHandler = new JsonHttpResponseHandler() {

        @Override
        public void onStart() {
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
            // called when response HTTP status is "200 OK"
            Logger.d("On Http Complete: " + jsonObject.toString());
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CREATE_USER_SUCCESS, null, "User Created", null);

        }



        @Override
        public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
            if (throwable!=null){
                Logger.e("Http error " + statusCode, throwable);
                _controlChannelEvent.onControlEvent(Constants.EventReturnState.CREATE_USER_ERROR, null, null, "User Could not be created RC=" + statusCode);
            }
            if (errorResponse!=null && errorResponse.length()>0){
                Logger.e("Http error " + statusCode + ": " + errorResponse.toString());
                _controlChannelEvent.onControlEvent(Constants.EventReturnState.CREATE_USER_ERROR, null, null, "User Could not be created RC=" + statusCode +": " + errorResponse.toString());

            }
        }

        @Override
        public void onRetry(int retryNo) {
            // called when request is retried
        }
    };

    // Start of WebSocketChannelEvents

    @Override
    public void onWebSocketOpen() {
        _isConnected = true;
        _controlChannelEvent.onControlEvent(Constants.EventReturnState.CONFIG_SUCCESS, null, "Control Channel connected", null);
    }

    @Override
    public void onWebSocketMessage(String message) {


        if (message!=null){
            try {
                JSONObject jsonObject = new JSONObject(message);
                if (!jsonObject.isNull("type")){
                    String type = jsonObject.getString("type");
                    Logger.d("onWebSocketMessage type " + type);
                    Logger.d("onWebSocketMessage message " + message);
                    if (type.equalsIgnoreCase("message")){
                        ContactModel loginuser = ContactsStaticDataModel.getLogInUser();
                        if (loginuser==null){
                            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CRITICAL_ERROR, null, "User not available", null);
                            return;
                        }
                        if (!jsonObject.isNull("to")){
                            JSONArray toUserArray = jsonObject.getJSONArray("to");
                            String toUser = toUserArray.getString(0);
                            Logger.d("onWebSocketMessage toUser "+ toUser);
                            Logger.d("onWebSocketMessage login user "+ loginuser.getIdTag());
                            String fromUser = jsonObject.getString("from");

                            Logger.d("onWebSocketMessage fromUser "+ fromUser);
                            if (loginuser!=null && loginuser.getIdTag()!=null && toUser!=null && toUser.equalsIgnoreCase(loginuser.getIdTag())){
                                String id = jsonObject.getString("id");
                                String t = jsonObject.getString("timestamp");
                                //_controlChannelEvent.onControlEvent(Constants.EventReturnState.NEW_IN_MESSAGE, loginuser, message, null);
                                messagingClient.updateEventListeners(Constants.EventReturnState.NEW_IN_MESSAGE, loginuser, message, fromUser+","+id+","+t);
                            } else {
                                //_controlChannelEvent.onControlEvent(Constants.EventReturnState.NEW_OUT_MESSAGE, loginuser, message, null);
                                messagingClient.updateEventListeners(Constants.EventReturnState.NEW_OUT_MESSAGE, loginuser, message, fromUser);
                            }
                            return;
                        }

                    } else if (type.equalsIgnoreCase("users")){

                        if (!jsonObject.isNull("users")){
                            JSONArray usersArray = jsonObject.getJSONArray("users");
                            List<ContactModel> contacts = new ArrayList<ContactModel>();
                            for (int i=0;i<usersArray.length();i++){
                                JSONObject userRecord = usersArray.getJSONObject(i);
                                ContactModel contact = new ContactModel();
                                String tagId = userRecord.getString("id");
                                contact.setIdTag(tagId.trim());
                                //long created = userRecord.getLong("created");
                                //long modified = userRecord.getLong("modified");
                                String firstName = userRecord.isNull("firstName")?null:userRecord.getString("firstName");
                                contact.setFirstName(firstName);
                                String lastName = userRecord.isNull("lastName")?null:userRecord.getString("lastName");
                                contact.setLastName(lastName);
                                JSONObject contactObject = userRecord.getJSONObject("contacts");

                                if (!jsonObject.isNull("mobile")) {
                                    JSONArray mobilePhoneArray = userRecord.getJSONArray("mobile");
                                    for (int j=0;i<mobilePhoneArray.length();j++){
                                        String mobile = mobilePhoneArray.optString(j, "");
                                        contact.setPhoneNumber(mobile);
                                        break;
                                    }
                                }

                                if (!jsonObject.isNull("email")) {
                                    JSONArray emailArray = userRecord.getJSONArray("email");
                                    for (int j=0;i<emailArray.length();j++){
                                        String email = emailArray.optString(j, "");
                                        contact.setEmail(email);
                                        break;
                                    }
                                }

                                JSONObject avatarObject = userRecord.getJSONObject("avatar");
                                if (avatarObject!=null) {
                                    if (!avatarObject.isNull("small")){
                                        String smallAvatar = avatarObject.getString("small");
                                        contact.setAvatarUrlSmall(smallAvatar);
                                    }
                                    if (!avatarObject.isNull("large")){
                                        String largeAvatar = avatarObject.getString("large");
                                        contact.setAvatarUrlLarge(largeAvatar);
                                    }

                                }
                                contacts.add(contact);
                            }
                            ContactsStaticDataModel.setAllContacts(contacts);
                            _controlChannelEvent.onControlEvent(Constants.EventReturnState.GET_USERS_SUCCESS, null, "Users Received", null);
                        }

                    } else if (type.equals("presence")) {
                        requestAllUsers(); // refresh users

                    } else if (type.equalsIgnoreCase("call")) {
                        //Incoming call
                        String from = jsonObject.getString("from");
                        int clientTag = jsonObject.isNull("clientTag")?jsonObject.getInt("id"):jsonObject.getInt("clientTag");
                        String callStatus = jsonObject.getString("status");

                        if (callStatus!=null && callStatus.equalsIgnoreCase("initiate")){
                            //handleIncomingCall(from, clientTag + "");
                            _controlChannelEvent.onControlEvent(Constants.EventReturnState.INCOMING_CALL_SIGNAL, null, from, clientTag);

                        } else if (callStatus!=null && callStatus.equalsIgnoreCase("cancel")) {
                            ContactModel contactModel = new ContactModel();
                            contactModel.setIdTag(from);
                            //bus.post(new BusEvents.ProcessCallCancelEvent(contactModel));
                            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CALL_CANCEL_SIGNAL, null, from, clientTag);
                        }

                    } else if (type.equals("error")) {
                        String errorMsg = jsonObject.getString("message");
                        if (errorMsg.indexOf("connection")>=0
                                ||errorMsg.indexOf("connection")>=0){
                            _controlChannelEvent.onControlEvent(Constants.EventReturnState.CONTROL_DISCONNECT, null, "Network connection error", null);
                        }

                    }

                } else {
                    _controlChannelEvent.onControlEvent(Constants.EventReturnState.OTHER_MESSAGE, null, message, null);

                }

            }catch (JSONException e) {
                Logger.e("Parse exception", e);
                _controlChannelEvent.onControlEvent(Constants.EventReturnState.CRITICAL_ERROR, null, "Message could not be parsed", null);

            }  catch (Exception e){
                _controlChannelEvent.onControlEvent(Constants.EventReturnState.CRITICAL_ERROR, null, "Error occurred "+e.toString(), null);
                Logger.e("Unhandled exception", e);
            }
        }
    }

    @Override
    public void onWebSocketClose() {
        _isConnected = false;
        _controlChannelEvent.onControlEvent(Constants.EventReturnState.CONTROL_DISCONNECT, null, "Control Connection Closed", null);

        if (!_shutdownRequested && isConnected()){
            retryLogin();
        }
    }

    private void retryLogin(){
        ContactModel contactModel = ContactsStaticDataModel.getLogInUser();
        if (contactModel!=null) {
            int connectionRetries = _channelConfig.getConnectionRetries();
            if (connectionRetries>_connectionRetries) {
                PnRTCManager.getInstance().connect(contactModel);
                _connectionRetries = connectionRetries+1;
            } else {
                _controlChannelEvent.onControlEvent(Constants.EventReturnState.CONTROL_RETRIES_FAILED, null, "Control retries failed", null);
            }

        } else {
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.LOGIN_REQUIRED, null, "Login required", null);
        }
    }

    @Override
    public void onWebSocketError(String description) {
        _isConnected = false;
        if (description.equalsIgnoreCase("eof")){
            _controlChannelEvent.onControlEvent(Constants.EventReturnState.FAULT_NOTIFICATION, null, description, null);
            return;
        }
        if (description.equalsIgnoreCase("http")){
            //_controlChannelEvent.onControlEvent(Constants.EventReturnState.FAULT_NOTIFICATION, null, description, null);
            retryLogin();
            return;
        }
        _controlChannelEvent.onControlEvent(Constants.EventReturnState.FAULT_NOTIFICATION, null, description, null);

    }

    @Override
    public void onNetworkStateChange(boolean isNetworkConnected, Constants.NetworkType type) {
        if (isNetworkConnected && _isNetworkConnected!=isNetworkConnected){
            ContactModel contactModel = ContactsStaticDataModel.getLogInUser();
            if (contactModel!=null && !_shutdownRequested) {
                connectControlChannel(contactModel);
            }
        }
        _isNetworkConnected = isNetworkConnected;
    }
    // End of WebSocketChannelEvents
}


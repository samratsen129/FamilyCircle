package com.familycircle.lib.utils;


//import android.util.Log;
import android.widget.Toast;

import com.familycircle.lib.AppRTCClient;
import com.familycircle.lib.AppRTCClient.WebSocketChannelEvents;
import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.sdk.BaseApp;
import com.familycircle.sdk.CallConstants;
import com.familycircle.sdk.models.CallModel;
import com.familycircle.sdk.models.ContactModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Negotiates signaling for chatting with apprtc.appspot.com "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class WebRTCClient implements AppRTCClient,
        WebSocketChannelEvents {


    private static final String TAG = "WSRTCClient";
    private static final String ROOM_JOIN = "join";
    private static final String ROOM_MESSAGE = "message";
    private static final String ROOM_LEAVE = "leave";

    private enum ConnectionState {
        NEW, CONNECTED, CLOSED, ERROR
    };
    private enum MessageType {
        MESSAGE, LEAVE
    };


    private final LooperExecutor executor;
    private boolean initiator;
    private SignalingEvents events;
    private PnRTCManager wsClient;
    private ConnectionState roomState;
    private RoomConnectionParameters connectionParameters;
    private CallModel callModel;

    public WebRTCClient(SignalingEvents events, LooperExecutor executor, CallModel callModel) {
        this.events = events;
        this.executor = executor;
        this.callModel = callModel;
        if (callModel.getCallType()== CallConstants.CallType.Incoming){
            initiator=true;
        } else {
            initiator=false;
        }
        roomState = ConnectionState.NEW;
    }

    // --------------------------------------------------------------------
    // AppRTCClient interface implementation.
    // Asynchronously connect to an AppRTC room URL using supplied connection
    // parameters, retrieves room parameters and connect to WebSocket server.
    @Override
    public void connectToRoom(RoomConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        wsClient = PnRTCManager.getInstance();
        wsClient.addListener(this);
        //ContactModel fromUser = callModel.getFromContact();
        //wsClient.connect(fromUser, TeamApp.getUrl(), initiator);

        executor.requestStart();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                connectToRoomInternal();
            }
        });
    }

    @Override
    public void disconnectFromRoom() {

        if (wsClient != null) {
            //wsClient.disconnect(true);
            wsClient.cancelCall(callModel);
            wsClient.removeListener(this);
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                disconnectFromRoomInternal();
            }
        });
        executor.requestStop();
    }

    // Connects to room - function runs on a local looper thread.
    private void connectToRoomInternal() {
        roomState = ConnectionState.NEW;
        //wsClient = new WebSocketChannelClient(executor, this);


        if (wsClient.STATUS != PnRTCManager.STATE.REGISTERED){
            Toast.makeText(BaseApp.getContext(), "Not connected", Toast.LENGTH_LONG).show();
        }

        WebRTCClient.this.executor.execute(new Runnable() {
            @Override
            public void run() {
                WebRTCClient.this.signalingParametersReady(wsClient.getSignallingParameters());
            }
        });

    }

    // Disconnect from room and send bye messages - runs on a local looper thread.
    private void disconnectFromRoomInternal() {
        Logger.d("Disconnect. Room state: " + roomState);
        roomState = ConnectionState.CLOSED;
        if (wsClient != null) {
            //wsClient.disconnect(true);
            //wsClient.cancelCall(callModel);
            wsClient.removeListener(this);
        }
    }


    // Callback issued when room parameters are extracted. Runs on local
    // looper thread.
    private void signalingParametersReady(
            final SignalingParameters signalingParameters) {
        Logger.d("Room connection completed.");
/*    if (connectionParameters.loopback
        && (!signalingParameters.initiator
            || signalingParameters.offerSdp != null)) {
      reportError("Loopback room is busy.");
      return;
    }
    if (!connectionParameters.loopback
        && !signalingParameters.initiator
        && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP in room response.");
    }*/
        //initiator = signalingParameters.initiator;
        roomState = ConnectionState.CONNECTED;

        // Fire connection and signaling parameters events.
        events.onConnectedToRoom(signalingParameters);
        if (!initiator){
            try {
                startCall();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Logger.d("Error in starting call", e);
            }
        }

    }

    public void startCall() throws Exception {
        ContactModel fromUser = callModel.getFromContact();
        ContactModel toUser = callModel.getToContact();
        String jsonMsg = "{\"type\":\"call\", \"to\":[\"" + toUser.getIdTag() + "\"], \"from\":\"" + fromUser.getIdTag() + "\", \"mode\":\"video\", \"status\":\"initiate\", \"clientTag\":"+callModel.getCallTagId()+"}";
        wsClient.transmitMessage(toUser.getIdTag(), new JSONObject(jsonMsg));

    }

    // Send local offer SDP to the other participant.
    @Override
    public void sendOfferSdp(final SessionDescription sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != ConnectionState.CONNECTED) {
                    reportError("Sending offer SDP in non connected state.");
                    return;
                }

                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "offer");
                ContactModel fromUser = callModel.getFromContact();
                ContactModel toUser = callModel.getToContact();
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(toUser.getIdTag());
                jsonPut(json, "to", jsonArray);
                jsonPut(json, "from", fromUser.getIdTag());
                jsonPut(json, "clientTag", callModel.getCallTagId());

                try {
                    wsClient.transmitMessage(toUser.getIdTag(), new JSONObject(json.toString()));
                } catch (Exception e){
                    Logger.e("Exception sendOfferSdp" + e.toString(), e);
                }

                if (connectionParameters.loopback) {
                    // In loopback mode rename this offer to answer and route it back.
                    SessionDescription sdpAnswer = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm("answer"),
                            sdp.description);
                    events.onRemoteDescription(sdpAnswer);
                }
            }
        });
    }

    // Send local answer SDP to the other participant.
    @Override
    public void sendAnswerSdp(final SessionDescription sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (connectionParameters.loopback) {
                    Logger.e("Sending answer in loopback mode.");
                    return;
                }
                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "answer");
                ContactModel fromUser = callModel.getFromContact();
                ContactModel toUser = callModel.getToContact();
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(toUser.getIdTag());
                jsonPut(json, "to", jsonArray);
                jsonPut(json, "from", fromUser.getIdTag());
                jsonPut(json, "clientTag", callModel.getCallTagId());
                try {
                    wsClient.transmitMessage(toUser.getIdTag(), new JSONObject(json.toString()));
                } catch (Exception e){
                    Logger.e("Exception sendOfferSdp" + e.toString(), e);
                }
            }
        });
    }

    // Send Ice candidate to the other participant.
    @Override
    public void sendLocalIceCandidate(final IceCandidate candidate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "candidate");
                jsonPut(json, "label", candidate.sdpMLineIndex);
                jsonPut(json, "id", candidate.sdpMid);
                jsonPut(json, "candidate", candidate.sdp);
                ContactModel fromUser = callModel.getFromContact();
                ContactModel toUser = callModel.getToContact();
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(toUser.getIdTag());
                jsonPut(json, "to", jsonArray);
                jsonPut(json, "from", fromUser.getIdTag());
                jsonPut(json, "clientTag", callModel.getCallTagId());

                if (initiator) {
                    // Call initiator sends ice candidates to GAE server.
                    if (roomState != ConnectionState.CONNECTED) {
                        reportError("Sending ICE candidate in non connected state.");
                        return;
                    }
                    //endPostMessage(MessageType.MESSAGE, messageUrl, json.toString());


                    if (connectionParameters.loopback) {
                        events.onRemoteIceCandidate(candidate);
                    }
                } else {
                    // Call receiver sends ice candidates to websocket server.
                    // wsClient.send(json.toString());
                }

                try {
                    wsClient.transmitMessage(toUser.getIdTag(),  new JSONObject(json.toString()));
                } catch (Exception e){
                    Logger.e("Exception sendOfferSdp" + e.toString(), e);
                }
            }
        });
    }

    // --------------------------------------------------------------------
    // WebSocketChannelEvents interface implementation.
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    @Override
    public void onWebSocketOpen() {
        Logger.d("Websocket connection completed. Registered...");
        //wsClient.register();
    }

    @Override
    public void onWebSocketMessage(final String msg) {
        if (wsClient.STATUS != PnRTCManager.STATE.REGISTERED) {
            Logger.e("Got WebSocket message in non registered state.");
            return;
        }
        try {

            if (msg.length() > 0) {
                JSONObject json = new JSONObject(msg);
                //String msgText = json.getString("msg");
                String errorText = json.optString("error");

                json = new JSONObject(msg);
                String type = json.optString("type");
                if (type.equals("candidate")) {
                    IceCandidate candidate = new IceCandidate(
                            json.getString("id"),
                            json.getInt("label"),
                            json.getString("candidate"));
                    events.onRemoteIceCandidate(candidate);
                } else if (type.equals("answer")) {
                    if (initiator) {
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type),
                                json.getString("sdp"));
                        events.onRemoteDescription(sdp);
                    } else {
                        reportError("Received answer for call initiator: " + msg);
                    }
                } else if (type.equals("offer")) {
                    if (!initiator) {
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type),
                                json.getString("sdp"));
                        events.onRemoteDescription(sdp);
                    } else {
                        reportError("Received offer for call receiver: " + msg);
                    }
                } else if (type.equals("bye")) {
                    events.onChannelClose();
                } else {
                    reportError("Unexpected WebSocket message: " + msg);
                }
            } else {
                reportError("Unexpected WebSocket message: " + msg);

            }
        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    @Override
    public void onWebSocketClose() {
        events.onChannelClose();
    }

    @Override
    public void onWebSocketError(String description) {
        reportError("WebSocket error: " + description);
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Logger.e(errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != ConnectionState.ERROR) {
                    roomState = ConnectionState.ERROR;
                    events.onChannelError(errorMessage);
                }
            }
        });
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Send SDP or ICE candidate to a room server.
/*  private void sendPostMessage(
      final MessageType messageType, final String url, final String message) {
    String logInfo = url;
    if (message != null) {
      logInfo += ". Message: " + message;
    }
    Log.d(TAG, "C->GAE: " + logInfo);
    AsyncHttpURLConnection httpConnection = new AsyncHttpURLConnection(
      "POST", url, message, new AsyncHttpEvents() {
        @Override
        public void onHttpError(String errorMessage) {
          reportError("GAE POST error: " + errorMessage);
        }

        @Override
        public void onHttpComplete(String response) {
          if (messageType == MessageType.MESSAGE) {
            try {
              JSONObject roomJson = new JSONObject(response);
              String result = roomJson.getString("result");
              if (!result.equals("SUCCESS")) {
                reportError("GAE POST error: " + result);
              }
            } catch (JSONException e) {
              reportError("GAE POST JSON error: " + e.toString());
            }
          }
        }
      });
    httpConnection.send();
  }*/

    public CallModel getCallModel() {
        return callModel;
    }

    public void setCallModel(CallModel callModel) {
        this.callModel = callModel;
    }
}

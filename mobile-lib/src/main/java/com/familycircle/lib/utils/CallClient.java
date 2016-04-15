package com.familycircle.lib.utils;

import android.os.Handler;
import android.util.Log;

import com.familycircle.lib.AppRTCClient;
import com.familycircle.lib.utils.db.SmsDbAdapter;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IAVCallChannelClient;
import com.familycircle.sdk.IAVCallChannelEvent;
import com.familycircle.sdk.models.CallModel;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by samratsen on 6/12/15.
 */
public class CallClient implements IAVCallChannelClient, AppRTCClient.SignalingEvents{


    private static final String TAG = "CallClient";
    private boolean muteOn;
    private boolean speakerOn;
    private int missedCallCount=0;
    private PeerConnectionClient2 peerConnectionClient;
    private AppRTCClient.SignalingParameters signalingParameters;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private boolean initiator = false;
    private Handler mHandler;

    private List<IAVCallChannelEvent> _eventListeners = new ArrayList<IAVCallChannelEvent>();

    private List<CallModel> activeCallItem; // this represents a session, in which multiple calls can be sent, received and held
    private List<CallModel> archivedCalls = new ArrayList<CallModel>();

    private static CallClient _instance = new CallClient();

    private CallClient(){
        activeCallItem = new ArrayList<CallModel>();
        setMuteOn(false);
        setSpeakerOn(false);
    }

    public static CallClient getInstance(){
        return _instance;
    }

    public void addAVCallEventListener(IAVCallChannelEvent eventListener, Handler handler){

        boolean found = false;
        mHandler = handler;
        for (IAVCallChannelEvent channelEvent:_eventListeners){
            if (eventListener == channelEvent) {
                found = true;
                break;
            }

        }
        if (!found){
            _eventListeners.add(eventListener);
        }
    }

    public void removeAVCallEventListener(IAVCallChannelEvent eventListener){
        //_eventListeners.remove(eventListener); double registration will cause issues
        mHandler = null;
        if (_eventListeners==null||_eventListeners.isEmpty()) return;
        Iterator<IAVCallChannelEvent> itr = _eventListeners.iterator();
        while(itr.hasNext()){
            IAVCallChannelEvent _event = itr.next();
            if (_event!=null && _event == eventListener){
                itr.remove();
            }
        }
    }

    public boolean isMuteOn() {
        return muteOn;
    }


    public void setMuteOn(boolean muteOn) {
        this.muteOn = muteOn;
    }


    public boolean isSpeakerOn() {
        return speakerOn;
    }


    public void setSpeakerOn(boolean speakerOn) {
        this.speakerOn = speakerOn;
    }

    public void updateEventListeners(final Constants.EventReturnState returnState, final CallModel callModel, final String msg, final Object extra){
        if (_eventListeners==null||_eventListeners.isEmpty()) return;

        if (returnState == Constants.EventReturnState.NEW_IN_MESSAGE){

        }

        for (IAVCallChannelEvent callEvent:_eventListeners){
            callEvent.onMessagingEvent(returnState, callModel, msg, extra);

        }
    }

    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        //if (logToast != null) {
        //    logToast.cancel();
        //}
    }

    /* Start of AppRTCClient.SignalingEvents*/
    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                /*final long delta = System.currentTimeMillis() - callStartedTimeMs;

                signalingParameters = params;
                if (peerConnectionClient == null) {
                    Log.w(TAG, "Room is connected, but EGL context is not ready yet.");
                    return;
                }
                Log.d(TAG, "Creating peer connection, delay=" + delta + "ms");
                peerConnectionClient.createPeerConnection(localRender, remoteRender,
                        signalingParameters, initiator);
                signalingParameters = peerConnectionClient.getSignalingParameters();
                if (signalingParameters.initiator) {
                    logAndToast("Creating OFFER...");
                    // Create offer. Offer SDP will be sent to answering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createOffer();
                } else {

                }*/
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        /*final long delta = System.currentTimeMillis() - callStartedTimeMs;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG,
                            "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta
                        + "ms");
                peerConnectionClient.setRemoteDescription(sdp);
                if (!signalingParameters.initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client
                    // in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
            }
        });*/
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG,
                            "Received ICE candidate for non-initilized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });*/
    }

    @Override
    public void onChannelClose() {
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });*/
    }

    @Override
    public void onChannelError(String description) {
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });*/

    }
    /*End of AppRTCClient.SignalingEvents */
}

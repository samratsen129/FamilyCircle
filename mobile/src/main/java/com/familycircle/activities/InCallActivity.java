package com.familycircle.activities;


import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
//import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.manager.TeamManager;
import com.familycircle.lib.AppRTCClient;
import com.familycircle.lib.OnCallEvents;
import com.familycircle.lib.utils.AppRTCAudioManager;
import com.familycircle.lib.utils.CpuMonitor;
import com.familycircle.lib.utils.Logger;
import com.familycircle.lib.utils.LooperExecutor;
import com.familycircle.lib.utils.PeerConnectionClient;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.lib.utils.UnhandledExceptionHandler;
import com.familycircle.lib.utils.WebRTCClient;
import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.sdk.CallConstants;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.CallModel;
import com.familycircle.sdk.models.ContactModel;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoRendererGui.ScalingType;


public class InCallActivity extends Activity implements
        AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents,
        OnCallEvents,
        Handler.Callback {

    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_CPUOVERUSE_DETECTION = "org.appspot.apprtc.CPUOVERUSE_DETECTION";
    public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    private static final String TAG = "CallRTCClient";
    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private boolean recordRemote = false;

    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private AppRTCClient.SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private ScalingType scalingType;
    private Toast logToast;
    private boolean commandLineRun;
    private int runTimeMs;
    private boolean activityRunning;
    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private CallFragment2 callFragment;
    private boolean initiator = false;
    // Controls
    private GLSurfaceView videoView;
    private ImageButton recordButton;
    private static CallModel callModel;

    public ProgressDialog mProgressDialog;
    public boolean shouldFinish = true;
    public boolean callEnded = false;
    private double audioSize = 0;
    private double videoSize = 0;
    private TextView recordSizeView;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TeamApp.startAudioRecording=false;
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(
                this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_in_call);

        iceConnected = false;
        signalingParameters = null;
        scalingType = ScalingType.SCALE_ASPECT_FILL;

        mHandler = new Handler(this);

        // Create UI controls.
        videoView = (GLSurfaceView) findViewById(R.id.glview_call);
        callFragment = new CallFragment2();

        //recordSizeView = (TextView) findViewById(R.id.record_size_text);

        // Create video renderers.
        VideoRendererGui.setView(videoView, new Runnable() {
            @Override
            public void run() {
                createPeerConnectionFactory();
            }
        });

        PrefManagerBase pref = new PrefManagerBase();
        if (pref.getIsLocalView()){
            recordRemote = false;
        }else {
            recordRemote = true;
        }

        if (recordRemote) {
            remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
            localRender = VideoRendererGui.create(LOCAL_X_CONNECTING,
                    LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                    LOCAL_HEIGHT_CONNECTING, scalingType, true);

        } else {
            localRender = VideoRendererGui.create(LOCAL_X_CONNECTING,
                    LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                    LOCAL_HEIGHT_CONNECTING, scalingType, false);
            remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, true);
        }

        // Show/hide call control fragment on view click.
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        });
        final Intent intent = getIntent();
        // Get Intent parameters.
        String roomId = "samratsen";

        String callType = intent.getStringExtra("CALL_TYPE");
        String calleeId = intent.getStringExtra("CALLEE_ID");
        callModel = new CallModel();
        ContactModel fromUser = PnRTCManager.getInstance().getUser();
        ContactModel toUser = new ContactModel();
        toUser.setIdTag(calleeId);
        callModel.setRoomId(roomId);
        callModel.setCallTagId(Integer.toString((new Random()).nextInt(100000000)));
        callModel.setFromContact(fromUser);
        callModel.setToContact(toUser);

        if (callType.equalsIgnoreCase("call")){
            callModel.setCallType(CallConstants.CallType.Outgoing);
            initiator=false;
        } else {
            callModel.setCallType(CallConstants.CallType.Incoming);
            initiator=true;
        }

		/*Uri roomUri = intent.getData();
		if (roomUri == null) {
			logAndToast(getString(R.string.missing_url));
			Log.e(TAG, "Didn't get any URL in intent!");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}*/
		/*
		String roomId = intent.getStringExtra(EXTRA_ROOMID);
		if (roomId == null || roomId.length() == 0) {
			logAndToast(getString(R.string.missing_url));
			Log.e(TAG, "Incorrect room ID in intent!");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}*/
        //boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);

        boolean loopback = false;
        boolean isVideoCall = true;
        String videoCode = "VP8";
        boolean hwCodecEnabled = true;
        String audioCodec ="OPUS";
        boolean cpuOveruseDetection = true;
        peerConnectionParameters = new PeerConnectionClient.PeerConnectionParameters(
                isVideoCall, loopback,
                0, 0, 0, 0,
                videoCode,
                hwCodecEnabled,
                0,
                audioCodec,
                cpuOveruseDetection);
        commandLineRun = false;
        runTimeMs = 0;

/*		Log.e(TAG,
				"Call Parameters EXTRA_VIDEO_CALL "
						+ intent.getBooleanExtra(EXTRA_VIDEO_CALL, true));
		Log.e(TAG, "Call Parameters LOOPBACK " + loopback);
		Log.e(TAG,
				"Call Parameters EXTRA_VIDEO_WIDTH "
						+ intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0));
		Log.e(TAG,
				"Call Parameters EXTRA_VIDEO_HEIGHT "
						+ intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0));
		Log.e(TAG,
				"Call Parameters EXTRA_VIDEO_FPS "
						+ intent.getIntExtra(EXTRA_VIDEO_FPS, 0));
		Log.e(TAG,
				"Call Parameters EXTRA_VIDEO_BITRATE "
						+ intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0));
		Log.e(TAG,
				"Call Parameters EXTRA_VIDEOCODEC "
						+ intent.getStringExtra(EXTRA_VIDEOCODEC));
		Log.e(TAG,
				"Call Parameters EXTRA_HWCODEC_ENABLED "
						+ intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true));
		Log.e(TAG,
				"Call Parameters EXTRA_AUDIO_BITRATE "
						+ intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0));
		Log.e(TAG,
				"Call Parameters EXTRA_AUDIOCODEC "
						+ intent.getStringExtra(EXTRA_AUDIOCODEC));
		Log.e(TAG,
				"Call Parameters EXTRA_CPUOVERUSE_DETECTION "
						+ intent.getBooleanExtra(EXTRA_CPUOVERUSE_DETECTION,
								true));
		Log.e(TAG,
				"Call Parameters EXTRA_CMDLINE "
						+ intent.getBooleanExtra(EXTRA_CMDLINE, false));
		Log.e(TAG, "Call Parameters EXTRA_RUNTIME " + runTimeMs);*/

        // Create connection client and connection parameters.
        appRtcClient = new WebRTCClient(this, new LooperExecutor(), callModel);
        //roomConnectionParameters = new RoomConnectionParameters(
        //		roomUri.toString(), roomId, loopback);
        roomConnectionParameters = new AppRTCClient.RoomConnectionParameters(null, roomId, loopback);
        // Send intent arguments to fragment.
        callFragment.setArguments(intent.getExtras());
        // Activate call fragment and start the call.
        getFragmentManager().beginTransaction()
                .add(R.id.call_fragment_container, callFragment).commit();
        startCall();

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            videoView.postDelayed(new Runnable() {
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }
    }

    // Activity interfaces
    @Override
    public void onPause() {
        TeamManager.getInstance().removeControlManagerClientHandler(mHandler);
        super.onPause();
        videoView.onPause();
        activityRunning = false;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
        TeamManager.getInstance().addControlManagerClientHandler(mHandler);
        activityRunning = true;
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
        try {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))));
        } catch (Exception e){

        }
       /* if (encoderInstance!=null){
            if (mProgressDialog!=null){
                mProgressDialog.show();
            } else {
                createDialog();
            }
        }*/
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
    }



    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onBackPressed(){
        return;
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
        } else {
            ft.hide(callFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void updateVideoView() {
        /*VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH,
                REMOTE_HEIGHT, scalingType);*/
        if (iceConnected) {
            if (recordRemote) {
                VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH,
                        REMOTE_HEIGHT, scalingType);
                VideoRendererGui.update(localRender, LOCAL_X_CONNECTED,
                        LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED,
                        LOCAL_HEIGHT_CONNECTED, ScalingType.SCALE_ASPECT_FIT);

            } else {
                VideoRendererGui.update(localRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH,
                        REMOTE_HEIGHT, scalingType);
                VideoRendererGui.update(remoteRender, LOCAL_X_CONNECTED,
                        LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED,
                        LOCAL_HEIGHT_CONNECTED, ScalingType.SCALE_ASPECT_FIT);
            }
            callFragment.enableRecordButton(true);

        } else {
            VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH,
                    REMOTE_HEIGHT, scalingType);
            VideoRendererGui.update(localRender, LOCAL_X_CONNECTING,
                    LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
                    LOCAL_HEIGHT_CONNECTING, scalingType);
        }
    }

    private void startCall() {
        if (appRtcClient == null) {
            Logger.e("AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        //logAndToast(getString(R.string.connecting_to,
        //		roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this, new Runnable() {
            // This method will be called each time the audio state (number and
            // type of devices) has been changed.
            @Override
            public void run() {
                onAudioManagerChangedState();
            }
        });
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Logger.d("Initializing the audio manager...");
        audioManager.init();
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Logger.i("Call connected: delay=" + delta + "ms");

        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if
        // AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    // Create peer connection factory when EGL context is ready.
    private void createPeerConnectionFactory() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    final long delta = System.currentTimeMillis()
                            - callStartedTimeMs;
                    Logger.d("Creating peer connection factory, delay="
                            + delta + "ms");
                    peerConnectionClient = new PeerConnectionClient();
                    peerConnectionClient.createPeerConnectionFactory(
                            InCallActivity.this,
                            VideoRendererGui.getEGLContext(),
                            peerConnectionParameters, InCallActivity.this);
                }
                if (signalingParameters != null) {
                    Logger.w("EGL context is ready after room connection.");
                    onConnectedToRoomInternal(signalingParameters);
                }
            }
        });
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        if (callFragment.isRecording) {
            //VideoRendererGui.getInstance().stopCapturing();
            callFragment.forceRecordClick();
        }
        try {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))));
        } catch (Exception e){

        }
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        callEnded = true;
        //if (callFragment.isRecording){
        //    callFragment.forceRecordClick();
        //}
        if (shouldFinish) {
            finish();
        }
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Logger.e("Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Connection error")
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            }).create().show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Logger.d(msg);
        if (logToast != null) {
            logToast.cancel();
        }
        //logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        //TextView v = (TextView) logToast.getView().findViewById(android.R.id.message);
        //v.setTextColor(Color.RED);
        //logToast.show();
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        if (peerConnectionClient == null) {
            Logger.w("Room is connected, but EGL context is not ready yet.");
            return;
        }
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        peerConnectionClient.createPeerConnection(localRender, remoteRender,
                signalingParameters, initiator);
        signalingParameters = peerConnectionClient.getSignalingParameters();
        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
			/*if (params.offerSdp != null) {
				peerConnectionClient.setRemoteDescription(params.offerSdp);
				logAndToast("Creating ANSWER...");
				// Create answer. Answer SDP will be sent to offering client in
				// PeerConnectionEvents.onLocalDescription event.
				peerConnectionClient.createAnswer();
			}
			if (params.iceCandidates != null) {
				// Add remote ICE candidates from room.
				for (IceCandidate iceCandidate : params.iceCandidates) {
					peerConnectionClient.addRemoteIceCandidate(iceCandidate);
				}
			}*/
        }
    }

    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Logger.e(
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
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Logger.e("Received ICE candidate for non-initilized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    // -----Implementation of
    // PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    logAndToast("Sending " + sdp.type + ", delay=" + delta
                            + "ms");
                    if (signalingParameters.initiator) {
                        appRtcClient.sendOfferSdp(sdp);
                    } else {
                        appRtcClient.sendAnswerSdp(sdp);
                    }
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
                iceConnected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError && iceConnected) {
                    callFragment.updateEncoderStatistics(reports);
                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    protected Dialog createDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Processing video file..");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        return mProgressDialog;
    }

    protected void dismissDialog(){
        if (mProgressDialog!=null && this!=null && mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }
    }

    /*public void refreshInfoView() {

        double currentSize = getCurrentSize();
        long availableSize = getAvailableSize();

        DecimalFormat formatter = new DecimalFormat("#0.00");

        if (recordSizeView!=null && recordSizeView.isShown()) recordSizeView.setText("Recording Size: " + formatter.format(currentSize) + "(MB) /" + availableSize + "(MB)");

    }

    public double getCurrentSize(){
        double currentSize = (TeamApp.audioBytes + TeamApp.videoBytes) / 1048576;
        return currentSize;
    }

    public long getAvailableSize(){
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSizeLong() * (long)stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / 1048576;
        return megAvailable;
    }*/

    @Override
    public boolean handleMessage(Message msg) {
        Logger.d("InCallActivity from Controller Login handleMessage");
        if (msg.what == Constants.EventReturnState.CALL_CANCEL_SIGNAL.value){
            disconnect();
        }
        return false;
    }

    public static class CallFragment2 extends Fragment {
        private View controlView;
        private TextView encoderStatView;
        private TextView roomIdView;

        private ImageButton disconnectButton;
        private ImageButton cameraSwitchButton;
        private ImageButton videoScalingButton;
        private ImageButton toggleDebugButton;
        private ImageButton recordButton;
        private OnCallEvents callEvents;
        private ScalingType scalingType;
        private boolean displayHud;
        private volatile boolean isRunning;
        private TextView hudView;
        private final CpuMonitor cpuMonitor = new CpuMonitor();
        private boolean isRecording = false;
        private InCallActivity activity;
        //private Animation animBlink = new AlphaAnimation(0.0f, 1.0f);

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            controlView = inflater.inflate(R.layout.fragment_in_call, container,
                    false);
            activity = (InCallActivity)getActivity();
            // Create UI controls.
            encoderStatView = (TextView) controlView
                    .findViewById(R.id.encoder_stat_call);
            roomIdView = (TextView) controlView
                    .findViewById(R.id.contact_name_call);

            hudView = (TextView) controlView.findViewById(R.id.hud_stat_call);
            disconnectButton = (ImageButton) controlView
                    .findViewById(R.id.button_call_disconnect);
            cameraSwitchButton = (ImageButton) controlView
                    .findViewById(R.id.button_call_switch_camera);
            videoScalingButton = (ImageButton) controlView
                    .findViewById(R.id.button_call_scaling_mode);
            toggleDebugButton = (ImageButton) controlView
                    .findViewById(R.id.button_toggle_debug);
            recordButton = (ImageButton) controlView
                    .findViewById(R.id.button_record);
            recordButton.setEnabled(false);
            // Add buttons click events.
            disconnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callEvents.onCallHangUp();
                    ContactModel contactModel = callModel.getToContact();
                    PnRTCManager.getInstance().onRejectIncomingCall(contactModel);
                }
            });

            cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callEvents.onCameraSwitch();
                }
            });

            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator;
                    String lastFileName = "video_capturing_"+ (new Date()).getTime() +".mp4";

                    if (!isRecording) {
                        isRecording = true;
                        // start enable and blink animation
                        //recordSizeView.setVisibility(View.VISIBLE);
                        //recordSizeView.setText("");
                        /*animBlink.setDuration(1000); //You can manage the blinking time with this parameter
                        animBlink.setStartOffset(20);
                        animBlink.setRepeatMode(Animation.REVERSE);
                        animBlink.setRepeatCount(Animation.INFINITE);
                        recordSizeView.startAnimation(animBlink);*/
                        // end enable and blink animation
                        recordButton.setBackgroundResource(R.drawable.record_stop);
                        //handlerRefresh.postDelayed(runnableRefresh, 2000);
                        try {
                            VideoRendererGui.getInstance().startCapturing(videoPath + lastFileName, getActivity());
                            //VideoCapturerAndroid.getInstance().startRecording();
                        } catch (Exception e) {
                            Logger.e("Error while starting video");
                        }
                    } else {
                        isRecording = false;
                        recordButton.setBackgroundResource(R.drawable.record_icon);
                        //if (handlerRefresh !=null) handlerRefresh.removeCallbacks(runnableRefresh);
                        try {
                            VideoRendererGui.getInstance().stopCapturing();
                            /*VideoCapturerAndroid.getInstance().stopRecording();
                            activity.startEncoder();*/
                        } catch (Exception e) {
                            Logger.e("Error while stopping video");
                        } finally {
                            //double currentSize = getCurrentSize();
                            //Toast.makeText(getActivity(), "Recording complete, size: " + currentSize, Toast.LENGTH_LONG);
                            // start reset of record size info and stop animation
                            //recordSizeView.setText("");
                            // recordSizeView.clearAnimation();
                            //recordSizeView.setVisibility(View.GONE);
                            // end reset of record size info and stop animation
                            try {
                                getActivity().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))));
                            } catch (Exception e){

                            }
                        }
                    }
                }
            });

            /*videoScalingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
                        videoScalingButton
                                .setBackgroundResource(R.drawable.ic_action_full_screen);
                        scalingType = ScalingType.SCALE_ASPECT_FIT;
                    } else {
                        videoScalingButton
                                .setBackgroundResource(R.drawable.ic_action_return_from_full_screen);
                        scalingType = ScalingType.SCALE_ASPECT_FILL;
                    }
                    callEvents.onVideoScalingSwitch(scalingType);
                }
            });*/
            scalingType = ScalingType.SCALE_ASPECT_FILL;

            /*toggleDebugButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (displayHud) {
                        int visibility = (hudView.getVisibility() == View.VISIBLE) ? View.INVISIBLE
                                : View.VISIBLE;
                        hudView.setVisibility(visibility);
                    }
                }
            });*/

            return controlView;
        }

        public void forceRecordClick(){
            recordButton.callOnClick();
        }

        public void enableRecordButton(boolean val){
            if (val)
                recordButton.setEnabled(true);
        }

        @Override
        public void onStart() {
            super.onStart();

            //Bundle args = getArguments();
            //if (args != null) {
            String roomId = callModel.getRoomId();
            if (callModel.getToContact()!=null) {
                roomIdView.setText("Call With: " + callModel.getToContact().getIdTag());
            }
            displayHud = false;
            //displayHud = args.getBoolean(InCallActivity2.EXTRA_DISPLAY_HUD,false);
            //}
            int visibility = displayHud ? View.VISIBLE : View.INVISIBLE;
            encoderStatView.setVisibility(visibility);
            toggleDebugButton.setVisibility(visibility);
            hudView.setVisibility(View.INVISIBLE);
            hudView.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
            isRunning = true;

            //handlerRefresh.postDelayed(runnableRefresh, 2000);
        }

        @Override
        public void onStop() {
            isRunning = false;
            super.onStop();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            callEvents = (OnCallEvents) activity;
        }

        private Map<String, String> getReportMap(StatsReport report) {
            Map<String, String> reportMap = new HashMap<String, String>();
            for (StatsReport.Value value : report.values) {
                reportMap.put(value.name, value.value);
            }
            return reportMap;
        }

        public void updateEncoderStatistics(final StatsReport[] reports) {
            if (!isRunning || !displayHud) {
                return;
            }
            String fps = null;
            String targetBitrate = null;
            String actualBitrate = null;
            StringBuilder bweBuilder = new StringBuilder();
            for (StatsReport report : reports) {
                if (report.type.equals("ssrc") && report.id.contains("ssrc")
                        && report.id.contains("send")) {
                    Map<String, String> reportMap = getReportMap(report);
                    String trackId = reportMap.get("googTrackId");
                    if (trackId != null
                            && trackId
                            .contains(PeerConnectionClient.VIDEO_TRACK_ID)) {
                        fps = reportMap.get("googFrameRateSent");
                    }
                } else if (report.id.equals("bweforvideo")) {
                    Map<String, String> reportMap = getReportMap(report);
                    targetBitrate = reportMap.get("googTargetEncBitrate");
                    actualBitrate = reportMap.get("googActualEncBitrate");

                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "")
                                .replace("Available", "")
                                .replace("Bandwidth", "")
                                .replace("Bitrate", "").replace("Enc", "");
                        bweBuilder.append(name).append("=").append(value.value)
                                .append(" ");
                    }
                    bweBuilder.append("\n");
                }
            }

            StringBuilder stat = new StringBuilder(128);
            if (fps != null) {
                stat.append("Fps:  ").append(fps).append("\n");
            }
            if (targetBitrate != null) {
                stat.append("Target BR: ").append(targetBitrate).append("\n");
            }
            if (actualBitrate != null) {
                stat.append("Actual BR: ").append(actualBitrate).append("\n");
            }

            if (cpuMonitor.sampleCpuUtilization()) {
                stat.append("CPU%: ").append(cpuMonitor.getCpuCurrent())
                        .append("/").append(cpuMonitor.getCpuAvg3())
                        .append("/").append(cpuMonitor.getCpuAvgAll());
            }
            encoderStatView.setText(stat.toString());
            hudView.setText(bweBuilder.toString() + hudView.getText());

        }


    }

/*    @Subscribe
    public void onIncomingAudioSizeEvent(BusEvents. IncomingAudioSizeEvent event) {
        //Log.d(TAG, "onIncomingAudioSizeEvent" + event.audioSize);
        if (callFragment!=null) {
            audioSize = event.audioSize;
            callFragment.refreshInfoView();
        }

    }

    @Subscribe
    public void onIncomingAudioSizeEvent(BusEvents. IncomingVideoSizeEvent event) {
        //Log.d(TAG, "onIncomingAudioSizeEvent" + event.videoSize);
        if (callFragment!=null) {
            videoSize = event.videoSize;
            callFragment.refreshInfoView();
        }
    }

    @Subscribe
    public void onCompleteVideoProcessingEvent(BusEvents.CompleteVideoProcessingEvent event) {
        //Log.d(TAG, "CompleteVideoProcessingEvent" + event.fileName);
        if (callFragment!=null) {
            String fileNm = event.fileName;
            SnackbarManager.show(
                    Snackbar.with(this)
                            .text("Video recording processing complete for " + fileNm));

        }
    }*/

}


package com.familycircle.activities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.custom.views.CircularImageView;
import com.familycircle.lib.utils.AudioPlayer;
import com.familycircle.lib.utils.Foreground;
import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.sdk.models.ContactModel;

import java.util.ArrayList;
import java.util.List;

public class IncomingCallActivity extends Activity implements View.OnClickListener{
    private Button ignoreNewCall;
    private Button answerEndCurrentCall;
    private Button answerHoldCurrentCall;
    private Button acceptButton;
    private Button rejectButton;
    private Button buttonMessage;
    private Button chatSendButton;
    private Button buttonOptionMessage1;
    private Button buttonOptionMessage2;
    private Button buttonOptionMessage3;
    private Button buttonOptionMessage4;
    private Button buttonOptionMessageCustom;
    private Button buttonCancelMessage;
    private TextView nameTextView;
    private TextView phoneTextView;
    private CircularImageView userImage;
    private List<Integer> receivingCallIds = new ArrayList<Integer>();;
    private PowerManager.WakeLock wakeLock;
    private int callState_callID = 0;
    private int currentCallIdKey;
    private String currentCallInfo;
    private String callState_info;
    private boolean isWindowInCall;
    private LinearLayout incomingCallLayout;
    private LinearLayout incomingActiveCallLayout;
    private final String TAG = "IncomingCallActivity";
    private ContactModel contactDetail;
    private boolean mblnTrayOverlayInflated = false;
    private boolean mblnTrayOverlayVisible = false;
    private String _sendMessageText;
    private String calledViewName;
    //private String callerId;
    private String calleeId;
    private String callType;
    private String callTagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        incomingActiveCallLayout = (LinearLayout) findViewById(R.id.newIncomingCall);
        incomingCallLayout = (LinearLayout) findViewById(R.id.incomingcall);

        acceptButton = (Button) findViewById(R.id.buttonAccept);
        acceptButton.setOnClickListener(this);

        rejectButton = (Button) findViewById(R.id.buttonReject);
        rejectButton.setOnClickListener(this);

        nameTextView = (TextView) findViewById(R.id.nameTextView);
        phoneTextView = (TextView) findViewById(R.id.phoneTextView);
        userImage = (CircularImageView) findViewById(R.id.cont_img);

        buttonMessage = (Button) findViewById(R.id.buttonMessage);
        buttonMessage.setOnClickListener(this);

        answerEndCurrentCall = (Button) findViewById(R.id.answerEndCurrentCall);
        SpannableString ss = new SpannableString(
                "Answer\nEnd Current Call");
        ss.setSpan(new AbsoluteSizeSpan(20, true), 0, 6, 0);
        ss.setSpan(new AbsoluteSizeSpan(14, true), 6, 23, 0);
        ss.setSpan(
                new ForegroundColorSpan(Color
                        .parseColor("#FB543F")), 6, 23, 0);
        answerEndCurrentCall.setText(ss);
        answerEndCurrentCall.setOnClickListener(this);

        answerHoldCurrentCall = (Button) findViewById(R.id.answerHoldCurrentCall);
        SpannableString ss1 = new SpannableString(
                "Answer\nHold Current Call");
        ss1.setSpan(new AbsoluteSizeSpan(20, true), 0, 6, 0);
        ss1.setSpan(new AbsoluteSizeSpan(14, true), 6, 24,
                0);
        ss1.setSpan(
                new ForegroundColorSpan(Color
                        .parseColor("#ffcc00")), 6, 24, 0);
        answerHoldCurrentCall.setText(ss1);
        answerHoldCurrentCall.setOnClickListener(this);

        ignoreNewCall = (Button) findViewById(R.id.ignoreNewCall);
        ignoreNewCall.setOnClickListener(this);

        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);

        Log.d(TAG, "IncomingActivity onCreate incoming call power manager set ");

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Window window = getWindow();
        //window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
        // window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager pm = (PowerManager) getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        wakeLock = pm
                .newWakeLock(
                        (PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP),
                        "TAG");
        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }

    @Override
    public void onResume() {
        // Register the upload receiver
        super.onResume();

        Intent intent = getIntent();
        if (intent != null) {
            String incomingCall = intent.getStringExtra("CALL_TYPE");
            calledViewName = intent.getStringExtra("ContactsFragment");
            if (incomingCall!=null && incomingCall.equalsIgnoreCase("join")) {
                Log.d(TAG, "IncomingCallActivity true");
                String _calleeId = intent.getStringExtra("CALLEE_ID");
                if (_calleeId != null) {
                    callType = incomingCall;
                    calleeId = _calleeId;
                }
                currentCallInfo = intent.getStringExtra("incomingCallInfo");

                acceptButton.setEnabled(true);
                rejectButton.setEnabled(true);
                Foreground.currentViewName = "IncomingCallActivity";
            } else {
                Log.d(TAG, "IncomingCallActivity false");
                finish();
            }
        }
    }

    @Override
    public void onPause() {

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        if (receivingCallIds != null) {
            receivingCallIds.clear();
            // receivingCallIds=null;
        }
    }

    @Override
    public void onBackPressed() {
        // Prevents the use of hardware back button
        // only in case if overlay is inflated remove it
        if (mblnTrayOverlayInflated && mblnTrayOverlayVisible)
        {
            findViewById(R.id.panel_import_overlay).setVisibility(View.GONE);
            mblnTrayOverlayVisible = false;
        }
    }

    @Override
    public void onAttachedToWindow() {
        // this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
        super.onAttachedToWindow();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonReject:
                try {
                    ContactModel contactModel = new ContactModel();
                    contactModel.setIdTag(calleeId);
                    PnRTCManager.getInstance().onRejectIncomingCall(contactModel);
                } catch (Exception e){

                } finally {
                    try {
                        AudioPlayer audioPlayer = AudioPlayer.getInstance();
                        audioPlayer.releasePlayer();

                    } catch (Exception e){
                        Log.d(TAG, "Issue while closing media player", e);
                    }
                    finish();
                }
                break;

            case R.id.buttonAccept:
                try {
                    Intent mainIntent = new Intent(TeamApp.getContext(), InCallActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mainIntent.putExtra("CALL_TYPE", "join");
                    // this is like a call from current user to the user who is calling
                    mainIntent.putExtra("CALLEE_ID", calleeId);
                    //mainIntent.putExtra("CALL_TAG", callTagId);
                    mainIntent.putExtra("incomingCallInfo", "IncomingCallActivity");
                    //mainIntent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                    startActivity(mainIntent);


                } catch (Exception e){
                    Log.e(TAG, "Issue invoking call activity", e);
                } finally {
                    try {
                        AudioPlayer audioPlayer = AudioPlayer.getInstance();
                        audioPlayer.releasePlayer();

                    } catch (Exception e){
                        Log.d(TAG, "Issue while closing media player", e);
                    }

                    finish();
                }
                break;

            case R.id.answerEndCurrentCall:
                break;

            case R.id.answerHoldCurrentCall:
                break;

            case R.id.ignoreNewCall:
                break;

            case R.id.buttonMessage:
                if (!mblnTrayOverlayInflated)
                {
                    ((ViewStub)findViewById(R.id.stub_import_overlay)).inflate();
                    mblnTrayOverlayInflated = true;
                    mblnTrayOverlayVisible = true;
                }
                else if (!mblnTrayOverlayVisible)
                {
                    findViewById(R.id.panel_import_overlay).setVisibility(View.VISIBLE);
                    mblnTrayOverlayVisible = true;
                }

                buttonOptionMessage1 = (Button) findViewById(R.id.buttonOptionMessage1);
                buttonOptionMessage1.setOnClickListener(this);
                buttonOptionMessage2 = (Button) findViewById(R.id.buttonOptionMessage2);
                buttonOptionMessage2.setOnClickListener(this);
                buttonOptionMessage3 = (Button) findViewById(R.id.buttonOptionMessage3);
                buttonOptionMessage3.setOnClickListener(this);
                buttonOptionMessage4 = (Button) findViewById(R.id.buttonOptionMessage4);
                buttonOptionMessage4.setOnClickListener(this);
                buttonOptionMessageCustom = (Button) findViewById(R.id.buttonOptionMessageCustom);
                buttonOptionMessageCustom.setOnClickListener(this);
                buttonCancelMessage  = (Button) findViewById(R.id.buttonCancelMessage);
                buttonCancelMessage.setOnClickListener(this);
                //endCall();
                break;

            case R.id.buttonCancelMessage:
                if (mblnTrayOverlayInflated && mblnTrayOverlayVisible)
                {
                    findViewById(R.id.panel_import_overlay).setVisibility(View.GONE);
                    mblnTrayOverlayVisible = false;
                }
                break;

            case R.id.buttonOptionMessage1:
            case R.id.buttonOptionMessage2:
            case R.id.buttonOptionMessage3:
            case R.id.buttonOptionMessage4:
                Button btn = (Button)v;
                if (btn!=null){
                    _sendMessageText = btn.getText().toString();
                    //sendMessage();
                }
                break;

            case R.id.buttonOptionMessageCustom:
                LinearLayout llchatSendView = (LinearLayout)findViewById(R.id.chatfooterrelativelayout);
                if (llchatSendView!=null){
                    llchatSendView.setVisibility(View.VISIBLE);
                    chatSendButton = (Button)findViewById(R.id.sendChatMsg);
                    chatSendButton.setOnClickListener(this);
                    EditText chatMessageArea = (EditText)findViewById(R.id.chatMessageArea);
                }
                break;

            case R.id.sendChatMsg:
                chatSendButton.setEnabled(false);
                chatSendButton.setAlpha(0.4f);
                EditText chatMessageArea = (EditText)findViewById(R.id.chatMessageArea);
                _sendMessageText = chatMessageArea.getText().toString();
                chatMessageArea.setEnabled(false);
                //sendMessage();
                break;
        }
    }
}

package com.familycircle.manager;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.activities.IncomingCallActivity;
import com.familycircle.utils.NotificationMgr;
import com.familycircle.utils.SmsNotificationMgr;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.lib.utils.AudioPlayer;
import com.familycircle.lib.utils.Foreground;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.sdk.BaseApp;
import com.familycircle.sdk.ChannelManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.IControlChannelEvent;
import com.familycircle.sdk.IMessagingChannelClient;
import com.familycircle.sdk.IMessagingChannelEvent;
import com.familycircle.sdk.models.ChannelConfig;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.MessageModel;

import java.util.ArrayList;

/**
 * Created by samratsen on 5/30/15.
 */
public final class TeamManager implements IControlChannelEvent, IMessagingChannelEvent {

    private static final String TAG = "TeamManager";
    private static TeamManager ourInstance = new TeamManager();

    // Messaging Client
    private IMessagingChannelClient messagingClient;

    // Channel Manager
    private ChannelManager teamChannelManager;

    // Channel Manager Client listeners
    private ChannelManagerController channelManagerController;

    // Messaging Client listeners
    private MessagingClientController messagingClientController;

    // WebRtc Client listeners
    private WebRtcClientController webrtcClientController;

    public static TeamManager getInstance() {
        return ourInstance;
    }

    private TeamManager() {

        messagingClientController = new MessagingClientController();
        channelManagerController = new ChannelManagerController();
        webrtcClientController = new WebRtcClientController();

        teamChannelManager = ChannelManager.getInstance();
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setApiKey("DUMMYAPIKEY#123");
        channelConfig.setConnectionRetries(5);

        // register this for receiving control signals
        teamChannelManager.startChannel(channelConfig, this);
        // register this for the Messaging client listenrs
        messagingClient = teamChannelManager.registerMessagingClient(this);
        if (messagingClient!=null){
            Log.d(TAG, "TeamManager returning messagingClient");
        } else {
            Log.e(TAG, "TeamManager returning messagingClient null");
        }

    }

    public boolean isConnected(){
        return teamChannelManager.isConnected();
    }

    public void addControlManagerClientHandler(Handler clientHandler){
        channelManagerController.addHandler(clientHandler);
    }

    public void removeControlManagerClientHandler(Handler clientHandler){
        channelManagerController.removeHandler(clientHandler);
    }

    public void addMessagingClientHandler(Handler clientHandler){
        messagingClientController.addHandler(clientHandler);
    }

    public void removeMessagingClientHandler(Handler clientHandler){
        messagingClientController.removeHandler(clientHandler);
    }

    public void addWebRtcClientHandler(Handler clientHandler){
        webrtcClientController.addHandler(clientHandler);
    }

    public void removeWebRtcClientHandler(Handler clientHandler){
        webrtcClientController.removeHandler(clientHandler);
    }

    public IMessagingChannelClient getMessagingClient(){
        if (messagingClient!=null){
            Log.e(TAG, "TeamManager getMessagingClient returning messagingClient");
        }
        return messagingClient;
    }

    public void sendCommandMessage(String command, String toUser){
        teamChannelManager.sendCommandMessage(command, toUser);
    }

    public IChannelManagerClient getChannelManager(){
        return teamChannelManager;
    }

    private void handleIncomingCall(String fromId, String callTag){
        //if (Foreground.instance.isBackground()){
        Intent mainIntent = new Intent(BaseApp.getContext(), IncomingCallActivity.class);
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

    @Override
    public void onControlEvent(Constants.EventReturnState returnState, ContactModel userContactModel, String msg, Object extra) {
        Log.d(TAG, "onControlEvent " + returnState +", " + userContactModel + ", " + msg + ", " + extra);
        if (returnState == Constants.EventReturnState.INCOMING_CALL_SIGNAL){
            final String callTag = ((Integer)extra).toString();
            handleIncomingCall(msg, callTag);
            return;
        }
        if (returnState == Constants.EventReturnState.INCOMING_COMMAND){
            Log.d(TAG, "Enabling command " + msg);
            if (msg.equalsIgnoreCase("enable_location")){
                PubSubManager.getInstance().startSharingLocation();

                NotificationMgr notificationMgr = new NotificationMgr();
                notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, "Alert", "", "Your location is being enabled " );

                return;
            }
            if (msg.equalsIgnoreCase("disable_location")){
                PubSubManager.getInstance().stopSharingLocation();
                return;
            }
            if (msg.equalsIgnoreCase("enable_vitals")){
                MBand2Manager.getInstance().startSubscriptionTask();
                NotificationMgr notificationMgr = new NotificationMgr();
                notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, "Alert", "", "Your microsoft band is being enabled ");

                return;
            }
            if (msg.equalsIgnoreCase("disable_vitals")){
                MBand2Manager.getInstance().pauseTasks();
                return;
            }
        }
        // route the control event to interested listeners
        channelManagerController.handleMessage(returnState.value, userContactModel, msg, extra);
    }

    @Override
    public void onMessagingEvent(Constants.EventReturnState returnState, ContactModel userContactModel, String msg, Object extra) {
        Log.d(TAG, "onMessagingEvent " + returnState +", " + userContactModel + ", " + msg + ", " + extra);
        if (returnState == Constants.EventReturnState.NEW_IN_MESSAGE){

            MessageModel messageModel = (MessageModel)extra;
            if (Foreground.get().isBackground()){
                //Notifications
                SmsNotificationMgr notificationMgr = new SmsNotificationMgr();
                notificationMgr.showNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID,
                        TeamApp.getContext().getString(R.string.new_notification_msg, messageModel.getName()),
                        messageModel.getTn(),
                        messageModel.getBody(),
                        messageModel.getMid());
            }
        }
        // route the messaging event to interested listeners
        messagingClientController.handleMessage(returnState.value, userContactModel, msg, extra);
    }
}

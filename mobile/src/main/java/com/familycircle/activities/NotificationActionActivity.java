package com.familycircle.activities;

import java.util.List;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.familycircle.TeamApp;
import com.familycircle.utils.NotificationMgr;
import com.familycircle.utils.TEAMConstants;

public class NotificationActionActivity extends Activity{
	public static final String TAG = "NotificationActionActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null)
		{
			int notificationId = intent.getIntExtra("NOTIFICATION_ID_PARM", 0);
			int notificationActionId = intent.getIntExtra("NOTIFICATION_ACTION_ID", 0);
			int callInternalId = intent.getIntExtra("NOTIFICATION_CALL_INTERNAL_ID", 0);
			Log.d(TAG, "Notification Service " + notificationId + ", " + callInternalId + "," + notificationActionId);
			
			if (notificationId>0)
			{
				switch (notificationId){
					case TEAMConstants.NOTIFICATION_INCOMING_CALL_ID:
						handleIncomingCallCmd(notificationActionId, callInternalId, notificationId);
						break;
						
					case TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID:
						handleActiveCallCmd(notificationActionId, callInternalId, notificationId);
						break;
				}
			}
			NotificationMgr.notificationInProgress = false;
		}
		finish();
	}
	
	private void handleIncomingCallCmd(int buttonId, int callInternalId, int notificationId){
		
		/*CallItem call = CallStateManager.getInstance().getCallItemBy_ID(callInternalId);
		if (call==null || call.callStatus == CallStatus.Completed || call.callType != CallType.Incoming){
			dismissNotification(notificationId);
			return;
		}
		
		switch (buttonId){
			case TEAMConstants.NOTIFICATION_INCOMING_ACCEPT_BUTTON_ID:
				CallStateManager.getInstance().acceptCall(call.callID);
				invokeCallScreen(callInternalId);
				break;
				
			case TEAMConstants.NOTIFICATION_INCOMING_REJECT_BUTTON_ID:
				CallStateManager.getInstance().rejectIncomingCall(call.callID);
				break;
		}
		dismissNotification(notificationId);*/
	}
	
	private void handleActiveCallCmd(int buttonId, int callInternalId, int notificationId){
		
		/*CallItem call = CallStateManager.getInstance().getCallItemBy_ID(callInternalId);
		if (call==null || call.callStatus == CallStatus.Completed *//*|| call.callType != CallType.Incoming*//*){
			dismissNotification(notificationId);
			return;
		}
		
		CallItem holdItem = CallStateManager.getInstance().getHoldCallItem();
		List<CallItem> activeItems = CallStateManager.getInstance().getActiveCallItems();
		int call_id = -1;
		switch (buttonId){
			case TEAMConstants.NOTIFICATION_ACTIVE_CALL_BUTTON_ID:
				//invokeMainScreen();
				if (activeItems!=null && !activeItems.isEmpty()){
					call_id = activeItems.get(0)._id;
					invokeCallScreen(call_id);
				} else if (holdItem!=null){
					call_id = holdItem._id;
					invokeCallScreen(holdItem._id);
				} else {
					invokeMainScreen();
				}
				
				break;
				
			case TEAMConstants.NOTIFICATION_ACTIVE_REJECT_BUTTON_ID:
				CallStateManager.getInstance().dropActiveCall();
				if (activeItems!=null && !activeItems.isEmpty() && holdItem!=null){
					call_id = activeItems.get(0)._id;
					invokeCallScreen(call_id);
				}
				break;
				
			case TEAMConstants.NOTIFICATION_ACTIVE_MUTE_BUTTON_ID:
				if (CallStateManager.getInstance().isMuteOn()){
					CallStateManager.getInstance().muteOff(call.phoneNum, call.callID);
				} else {
					CallStateManager.getInstance().muteOn(call.phoneNum, call.callID);
				}
				break;
				
			case TEAMConstants.NOTIFICATION_ACTIVE_SPEAKER_BUTTON_ID:
				if (CallStateManager.getInstance().isSpeakerOn()){
					CallStateManager.getInstance().speakerOff(call.phoneNum, call.callID);
				} else {
					CallStateManager.getInstance().speakerOn(call.phoneNum, call.callID);
				} 
				break;
		}*/
	}
	
	private void dismissNotification(int notificationId){
		NotificationMgr.cancelNotification(notificationId);
	}
	
	private void invokeCallScreen(int callInternalId){
		Intent intent = new Intent(TeamApp.getContext(), IncomingCallActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("incoming", false);
		intent.putExtra("incomingCallId", callInternalId);
		intent.putExtra("incomingCallInfo", "LandingPageActivity");
		intent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		TeamApp.getContext().startActivity(intent);
	}
	
	private void invokeMainScreen(){
		Intent intent = new Intent(TeamApp.getContext(), MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		TeamApp.getContext().startActivity(intent);
	}
}

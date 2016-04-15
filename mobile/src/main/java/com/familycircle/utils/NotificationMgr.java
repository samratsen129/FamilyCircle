package com.familycircle.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.activities.MainActivity;
import com.familycircle.lib.utils.Foreground;

public class NotificationMgr {
	private NotificationManagerCompat mNotificationManager;
	private NotificationCompat.Builder mNotificationBuilder;
	private Notification mNotification;
	private RemoteViews mNotificationContentView;
	private static Intent lastIntent = null;
	public static boolean notificationInProgress = false;

	private void initNotification(int notificationId, int layoutId, String message, String fromName)
	{
		if (mNotificationManager == null)
		{
			mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
		}

		if (mNotificationBuilder==null){

			Intent notificationIntent = new Intent(TeamApp.getContext(), MainActivity.class);
			notificationIntent.putExtra(TEAMConstants.NOTIFICATION_GROUP_DIALER, notificationId);
			PendingIntent contentIntent = PendingIntent.getActivity(TeamApp.getContext(), 0, notificationIntent, notificationId);

			String contentMsg = message.length()>15?message.substring(0, 15)+"...":message;

			String title = null;
			if (notificationId == TEAMConstants.NOTIFICATION_MISSED_CALL_ID){
				title = "Update from " + fromName;
			} else {
				title = "New voicemail from " + fromName;
			}
			mNotificationBuilder = new NotificationCompat.Builder(
					TeamApp.getContext())
			.setSmallIcon(R.drawable.notification_icon)
			.setContentTitle(title)
			.setContentText(contentMsg)
			.setAutoCancel(true)
			.setContentIntent(contentIntent);
			//.setGroup(SCConstants.NOTIFICATION_GROUP_VMAIL);

		}

		if (mNotification==null){
			BigTextStyle bigStyle = new BigTextStyle();
			bigStyle
			.bigText(message);


			Bitmap bm = BitmapFactory.decodeResource(TeamApp.getContext().getResources(), R.drawable.wearable_bg);

			mNotification = mNotificationBuilder
					.extend(new WearableExtender()
					.setBackground(bm))
					.setStyle(bigStyle)
					.build();

		}

		if (mNotificationContentView==null) mNotificationContentView = new RemoteViews(TeamApp.getContext().getPackageName(),
				layoutId);

	}

	public void showNotification(int notificationId, 
			String title,
			String fromName,
			String messageText)
	{


		if (notificationId == TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID){

			cancelNotification(TEAMConstants.NOTIFICATION_MISSED_CALL_ID);

			initNotification(TEAMConstants.NOTIFICATION_MISSED_CALL_ID, R.layout.notification_call, messageText, fromName);

			if (Foreground.get().isBackground()) mNotification.defaults |= Notification.DEFAULT_SOUND;

			mNotificationContentView.setImageViewResource(R.id.notification_image, R.drawable.notification_icon);
			mNotificationContentView.setTextViewText(R.id.notification_title, title);
			String alertMsg = messageText.length()>60?messageText.substring(0, 60)+"...":messageText;
			mNotificationContentView.setTextViewText(R.id.notification_message, alertMsg);
			mNotification.contentView = mNotificationContentView;

			mNotificationManager.notify(TEAMConstants.NOTIFICATION_MISSED_CALL_ID, mNotification);

			//sendNewMissedCallMessage();

			// Badge update is happening from CallStateManager
			// SCUtils.updateDialerAppBadge(CallStateManager.getInstance().getMissedCallCount());
		}
	}

	public static void showIncomingCallNotification(int notificationId, int callInternalId)
	{
			/*CallItem callItem = CallStateManager.getInstance().getCallItemBy_ID(callInternalId);
			if (callItem==null) return;
			notificationInProgress = true;
			cancelNotification(TEAMConstants.NOTIFICATION_INCOMING_CALL_ID);
			
			String messageText = callItem.phoneNum;
			String userName = SCUtils.getContactNameFromTn(SCUtils.getPhoneNumberDigits(messageText));
			if (userName!=null && !userName.isEmpty()){
				messageText = userName;
			} else {
				messageText = SCUtils.getFormattedPhoneNumber(messageText);
			}
			
			Intent notificationAcceptService = new Intent(TeamApp.getContext().getApplicationContext(),
			        									NotificationActionActivity.class);
			notificationAcceptService.setAction("actionstring.accept" + callItem._id);
			notificationAcceptService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_INCOMING_CALL_ID);
			notificationAcceptService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_INCOMING_ACCEPT_BUTTON_ID);
			notificationAcceptService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationAcceptService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingAcceptService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 10, notificationAcceptService, 0);
			
			
			Intent notificationRejectService = new Intent(TeamApp.getContext().getApplicationContext(), NotificationActionActivity.class);
			notificationRejectService.setAction("actionstring.reject" + callItem._id);
			notificationRejectService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_INCOMING_CALL_ID);
			notificationRejectService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_INCOMING_REJECT_BUTTON_ID);
			notificationRejectService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationRejectService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingRejectService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 11, notificationRejectService, 0);
			
			NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
			
			RemoteViews mNotificationContentView = new RemoteViews(TeamApp.getContext().getPackageName(),
					R.layout.notification_incoming_call);
			mNotificationContentView.setOnClickPendingIntent(
				        R.id.buttonAcceptIncomingNotification,
				        pendingAcceptService);
			mNotificationContentView.setOnClickPendingIntent(
				        R.id.buttonRejectIncomingNotification,
				        pendingRejectService);
			mNotificationContentView.setTextViewText(R.id.tvCallInfoIncomingNotification, "Incoming Call " + messageText);

			NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(
											TeamApp.getContext())
											.setSmallIcon(R.drawable.notification_icon)
											.setContentTitle("Incoming Call from " + messageText)
											.setContentText(" ")
											//.setAutoCancel(true)
											.setContent(mNotificationContentView);
			
			Notification mNotification = mNotificationBuilder.build();
			
			
			
			if (Foreground.get().isBackground()) mNotification.defaults |= Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS;
			
			
			//mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
			//mNotification.contentView = mNotificationContentView;
			
			mNotificationManager.notify(TEAMConstants.NOTIFICATION_INCOMING_CALL_ID, mNotification);*/


	}
	
	
	public static void showActiveCallNotification(int notificationId, int callInternalId, boolean isMuteOn, boolean isSpeakerOn)
	{
			/*CallItem callItem = CallStateManager.getInstance().getCallItemBy_ID(callInternalId);
			if (callItem==null) return;
			notificationInProgress = true;
			cancelNotification(TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID);
			
			String messageText = callItem.phoneNum;
			String userName = SCUtils.getContactNameFromTn(SCUtils.getPhoneNumberDigits(messageText));
			if (userName!=null && !userName.isEmpty()){
				messageText = userName;
			} else {
				messageText = SCUtils.getFormattedPhoneNumber(messageText);
			}
			
			Intent notificationcallService = new Intent(TeamApp.getContext().getApplicationContext(),
			        									NotificationActionActivity.class);
			notificationcallService.setAction("actionstring.call" + callItem._id);
			notificationcallService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID);
			notificationcallService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_ACTIVE_CALL_BUTTON_ID);
			notificationcallService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationcallService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingCallService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 1, notificationcallService, 0);

			
			Intent notificationRejectService = new Intent(TeamApp.getContext().getApplicationContext(), NotificationActionActivity.class);
			notificationRejectService.setAction("actionstring.callreject" + callItem._id);
			notificationRejectService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID);
			notificationRejectService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_ACTIVE_REJECT_BUTTON_ID);
			notificationRejectService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationRejectService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingRejectService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 2, notificationRejectService, 0);
			
			
			Intent notificationMuteService = new Intent(TeamApp.getContext().getApplicationContext(), NotificationActionActivity.class);
			notificationMuteService.setAction("actionstring.callmute" + callItem._id);
			notificationMuteService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID);
			notificationMuteService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_ACTIVE_MUTE_BUTTON_ID);
			notificationMuteService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationMuteService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingMuteService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 3, notificationMuteService, 0);
			
			Intent notificationSpeakerService = new Intent(TeamApp.getContext().getApplicationContext(), NotificationActionActivity.class);
			notificationSpeakerService.setAction("actionstring.callspeaker" + callItem._id);
			notificationSpeakerService.putExtra("NOTIFICATION_ID_PARM", TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID);
			notificationSpeakerService.putExtra("NOTIFICATION_ACTION_ID", TEAMConstants.NOTIFICATION_ACTIVE_SPEAKER_BUTTON_ID);
			notificationSpeakerService.putExtra("NOTIFICATION_CALL_INTERNAL_ID", callItem._id);
			notificationSpeakerService.addCategory(Intent.CATEGORY_DEFAULT);
			PendingIntent pendingSpeakerService = PendingIntent.getActivity(
														TeamApp.getContext().getApplicationContext(), 4, notificationSpeakerService, 0);
			
			NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
			
			RemoteViews mNotificationContentView = new RemoteViews(TeamApp.getContext().getPackageName(),
																	R.layout.notification_active_call);
			mNotificationContentView.setOnClickPendingIntent(
				        R.id.buttonCallActiveNotification,
				        pendingCallService);
			mNotificationContentView.setOnClickPendingIntent(
				        R.id.buttonRejectActiveNotification,
				        pendingRejectService);
			mNotificationContentView.setOnClickPendingIntent(
			        R.id.buttonMuteActiveNotification,
			        pendingMuteService);
			if (isMuteOn){
				mNotificationContentView.setTextColor(R.id.buttonMuteActiveNotification, Color.parseColor("#a4c639"));
			} else {
				mNotificationContentView.setTextColor(R.id.buttonMuteActiveNotification, Color.parseColor("#ffffff"));
			}
			
			mNotificationContentView.setOnClickPendingIntent(
			        R.id.buttonSpeakerActiveNotification,
			        pendingSpeakerService);
			if (isSpeakerOn){
				mNotificationContentView.setTextColor(R.id.buttonSpeakerActiveNotification, Color.parseColor("#a4c639"));
			} else {
				mNotificationContentView.setTextColor(R.id.buttonSpeakerActiveNotification, Color.parseColor("#ffffff"));
			}
			mNotificationContentView.setTextViewText(R.id.tvCallInfoActiveNotification, "In call: " + messageText);

			NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(
											TeamApp.getContext())
											.setSmallIcon(R.drawable.notification_icon)
											.setContentTitle("Ongoing Call from " + messageText)
											.setContentText(" ")
											//.setAutoCancel(true)
											.setContent(mNotificationContentView);
			
			Notification mNotification = mNotificationBuilder.build();
			
			
			
			if (Foreground.get().isBackground()) mNotification.defaults |= Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS;
			
			
			//mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
			//mNotification.contentView = mNotificationContentView;
			
			mNotificationManager.notify(TEAMConstants.NOTIFICATION_ACTIVE_HOLD_CALL_ID, mNotification);*/


	}

	public static void cancelNotification(int notificationId)
	{
		NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
		if (mNotificationManager == null)
		{

			mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
		}

		mNotificationManager.cancel(notificationId);
		
		if (lastIntent!=null){
			TeamApp.getContext().removeStickyBroadcast(lastIntent);
			lastIntent = null;
		}

	}

	public static void sendNewMissedCallMessage(){

		Intent intent = new Intent(TEAMConstants.NOTIFICATION_NEW_DIALER_ACTION);
		lastIntent = intent;
		if (TeamApp.getContext()!=null){
			TeamApp.getContext().sendStickyBroadcast(intent);
		}
	}

	public static void cancelNewNotificationBroadcast(){
		if (lastIntent!=null){
			TeamApp.getContext().removeStickyBroadcast(lastIntent);
			lastIntent = null;
		}
	}


}

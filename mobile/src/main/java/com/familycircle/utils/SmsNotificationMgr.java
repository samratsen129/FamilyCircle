package com.familycircle.utils;

import java.util.Date;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.activities.MainActivity;
import com.familycircle.activities.MessageDetailActivity;
import com.familycircle.lib.utils.Foreground;
import com.familycircle.sdk.models.ContactsStaticDataModel;

public class SmsNotificationMgr {
	private NotificationManagerCompat mNotificationManager;
	private NotificationCompat.Builder mNotificationBuilder;
	private Notification mNotification;
	private RemoteViews mNotificationContentView;
	private static Intent lastIntent = null;
	
	private void initNotification(int notificationId, int layoutId, String message, String tn, int mid)
	{
		if (mNotificationManager == null)
		{
			
			/*mNotificationManager = (NotificationManager) SC_ApplicationClass.getContext().getApplicationContext()
			        .getSystemService(SC_ApplicationClass.getContext().NOTIFICATION_SERVICE);*/
			mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
		}
		
		if (mNotificationBuilder==null){
			
			Intent notificationIntent = new Intent(TeamApp.getContext(), MessageDetailActivity.class);
			notificationIntent.putExtra("from_notif", true);
			notificationIntent.putExtra("TAG_ID", tn);
			notificationIntent.putExtra("mid", mid);
			notificationIntent.putExtra("unreadCnt", 1);
			notificationIntent.putExtra("tn", tn);
			PendingIntent contentIntent = PendingIntent.getActivity(TeamApp.getContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			String contentMsg = message.length()>15?message.substring(0, 15)+"...":message;
			if (message.length()>15){
				contentMsg = "Page 1 \n" + contentMsg;
			}
			String fromName = ContactsStaticDataModel.getContactByIdTag(tn).getName();
			mNotificationBuilder = new NotificationCompat.Builder(
						TeamApp.getContext())
							.setSmallIcon(R.drawable.notification_icon)
							.setContentTitle("New Text from " + fromName)
							.setContentText(contentMsg)
							.setAutoCancel(true)
							.setContentIntent(contentIntent);
							//.setGroup(SCConstants.NOTIFICATION_GROUP_SMS);

		}
		
		if (mNotification==null && message.length()>15){
			BigTextStyle secondPageStyle = new BigTextStyle();
											secondPageStyle.setBigContentTitle("Page 2")
											.bigText(message);
			Notification secondPageNotification =
											        new NotificationCompat.Builder(TeamApp.getContext())
											        .setStyle(secondPageStyle)
											        .build();
			
			Bitmap bm = BitmapFactory.decodeResource(TeamApp.getContext().getResources(), R.drawable.wearable_bg);
			
			 mNotification = new WearableExtender()
							            .addPage(secondPageNotification)
							            .setBackground(bm)
							            .extend(mNotificationBuilder)
							            .build();
			
			        
		 } else {
			 mNotification = mNotificationBuilder.build();
		 }
		
		if (mNotificationContentView==null) mNotificationContentView = new RemoteViews(TeamApp.getContext().getPackageName(),
																						layoutId);

	}
	
	public void showNotification(int notificationId, 
										String title,
										String tn,
											String messageText, 
												int mid)
	{
		Log.d("SmsNotification", "showNotification");
		
		if (notificationId == TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID){
			
			//cancelNotification(SCConstants.NOTIFICATION_NEW_SMS_ID);
			
			initNotification(TEAMConstants.NOTIFICATION_INFO_MESSAGE_ID, R.layout.notification_sms, messageText, tn, mid);

			if (Foreground.get().isBackground()) {
				long currentTime = (new Date()).getTime();
				if ((currentTime-TeamApp.lastNotificationSound)>10*1000){
					mNotification.defaults |= Notification.DEFAULT_SOUND;
					TeamApp.lastNotificationSound = currentTime;
				}
			}

			mNotificationContentView.setImageViewResource(R.id.notification_image, R.drawable.notification_icon);
			mNotificationContentView.setTextViewText(R.id.notification_title, title);
			String alertMsg = messageText.length()>60?messageText.substring(0, 60)+"...":messageText;
			mNotificationContentView.setTextViewText(R.id.notification_message, alertMsg);
			mNotification.contentView = mNotificationContentView;
			
			mNotificationManager.notify(TEAMConstants.NOTIFICATION_NEW_SMS_ID, mNotification);
			
			//sendNewSmsMessage();
			
			TEAMUtils.updateSmsAppBadge();
		}
	}
	
	
	public void cancelNotification(int notificationId)
	{
		if (mNotificationManager == null)
		{
			
			/*mNotificationManager = (NotificationManager) SC_ApplicationClass.getContext().getApplicationContext()
			        .getSystemService(SC_ApplicationClass.getContext().NOTIFICATION_SERVICE);*/
			
			mNotificationManager = NotificationManagerCompat.from(TeamApp.getContext());
		}
		
		mNotificationManager.cancel(notificationId);
		if (lastIntent!=null){
			TeamApp.getContext().removeStickyBroadcast(lastIntent);
			lastIntent = null;
		}

	}
	
	public static void sendNewSmsMessage(){
		
		Intent intent = new Intent(TEAMConstants.NOTIFICATION_NEW_SMS_ACTION);
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

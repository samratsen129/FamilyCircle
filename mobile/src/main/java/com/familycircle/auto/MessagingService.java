package com.familycircle.auto;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.CarExtender;
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.activities.LoginActivity;
import com.familycircle.manager.TeamManager;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.TEAMUtils;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.lib.utils.db.SmsDbAdapter;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IMessagingChannelClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.MessageModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MessagingService extends Service  implements Handler.Callback{
    private static final String TAG = MessagingService.class.getSimpleName();
    private static final String EOL = "\n";
    private static final String READ_ACTION =
            "com.example.android.messagingservice.ACTION_MESSAGE_READ";

    public static final String REPLY_ACTION =
            "com.example.android.messagingservice.ACTION_MESSAGE_REPLY";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String CONVERSATION_TN = "conversation_tn";
    public static final String CONVERSATION_NAME = "conversation_name";
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    private static final AtomicInteger counter = new AtomicInteger();

    private NotificationManagerCompat mNotificationManager;

    private Handler mHandler;
    private IMessagingChannelClient messagingChannelClient;

    public final String AUTOPREFERENCES = "Auto_prefs" ;
    SharedPreferences sharedpreferences;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
        mHandler = new Handler(this);
        TeamManager.getInstance().addMessagingClientHandler(mHandler);
        messagingChannelClient = TeamManager.getInstance().getMessagingClient();

        sharedpreferences = getSharedPreferences(AUTOPREFERENCES, Context.MODE_PRIVATE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        String message =intent.getStringExtra("reply_message");
        if (message != null && !message.trim().isEmpty()) {
            handleCommand(intent);
        }
        if (intent.getBooleanExtra("read_message", false)) {
            readCountUpdate(intent);
        }
        return START_STICKY;
    }
    private void handleCommand(Intent intent) {
        Log.d(TAG, "handleCommand");
        String reply_message =intent.getStringExtra("reply_message");
        String conversation_tn = intent.getStringExtra(MessagingService.CONVERSATION_TN);
        String conversation_name = intent.getStringExtra(MessagingService.CONVERSATION_NAME);

        if (!NetworkUtils.IsNetworkAvailable(this)) {
            Toast.makeText(getApplicationContext(), "Unable to send message, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TeamManager.getInstance().isConnected()) {
            Toast.makeText(getApplicationContext(), "Warning: Network Connection to TEAM lost, message cannot be delivered", Toast.LENGTH_LONG).show();
            return;
        }

        if (reply_message == null || reply_message.trim().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please type a message to send", Toast.LENGTH_SHORT).show();

        } else {

            Iterator iter = LoginActivity.linkedHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry mEntry = (Map.Entry) iter.next();
                if(mEntry.getKey().toString().equalsIgnoreCase(reply_message)){
                    reply_message = mEntry.getValue().toString();
                }
            }

            if(!LoginActivity.linkedHashMap.isEmpty()) {
                Random rand = new Random();
                int randomNum = rand.nextInt(LoginActivity.linkedHashMap.size() - 0);
                reply_message = (new ArrayList<String>(LoginActivity.linkedHashMap.values())).get(randomNum);
            }

            MessageModel messageModel = new MessageModel();
            PrefManagerBase prefMgr = new PrefManagerBase();
            messageModel.setMid(prefMgr.getNextId());
            messageModel.setTn(conversation_tn);
            messageModel.setDir("O");
            messageModel.setBody(reply_message);
            //messageModel.setServerId(id);
            messageModel.setStatus("R");
            messageModel.setTime((new Date()).getTime());
            messageModel.setName(conversation_name);

            List<MessageModel> messageModels = new ArrayList<MessageModel>();
            messageModels.add(messageModel);

            messagingChannelClient.sendMessage(messageModels);
        }
    }
    private void readCountUpdate(Intent intent) {
        String conversation_tn = intent.getStringExtra(MessagingService.CONVERSATION_TN);
        SmsDbAdapter dbAdapter = new SmsDbAdapter();
        dbAdapter.updateMessageStatusByTn(conversation_tn, "R");
        TEAMUtils.updateSmsAppBadge();
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    // Creates an intent that will be triggered when a message is marked as read.
    private Intent getMessageReadIntent(int id, String contact_tn, String contact_name) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(READ_ACTION)
                .putExtra(CONVERSATION_ID, id)
                .putExtra(CONVERSATION_TN, contact_tn)
                .putExtra(CONVERSATION_NAME, contact_name);
    }

    // Creates an Intent that will be triggered when a voice reply is received.
    private Intent getMessageReplyIntent(int conversationId, String contact_tn, String contact_name) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(REPLY_ACTION)
                .putExtra(CONVERSATION_ID, conversationId)
                .putExtra(CONVERSATION_TN, contact_tn)
                .putExtra(CONVERSATION_NAME, contact_name);
    }

    private void sendNotificationForConversation(MessageModel message, int notification_Id) {
        // A pending Intent for reads
        PendingIntent readPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                notification_Id,
                getMessageReadIntent(notification_Id, message.getTn(), message.getName()),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Build a RemoteInput for receiving voice input in a Car Notification
        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                .setLabel( "Reply" )
                .build();

        // Building a Pending Intent for the reply action to trigger
        PendingIntent replyIntent = PendingIntent.getBroadcast(getApplicationContext(),
                notification_Id,
                getMessageReplyIntent(notification_Id, message.getTn(), message.getName()),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap bitmap;
        final ContactModel contactModel = ContactsStaticDataModel.getContactByIdTag(message.getTn());
        if (contactModel!=null && contactModel.getAvatarUrlSmall()!=null && contactModel.getAvatarUrlSmall().indexOf("http")>=0){
            bitmap = getBitmapFromURL(contactModel.getAvatarUrlSmall());
        } else {
            bitmap = BitmapFactory.decodeResource(
                    getApplicationContext().getResources(), R.drawable.icon_avatar);
        }

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        UnreadConversation.Builder unreadConvBuilder =
                new UnreadConversation.Builder(message.getName())
                .setLatestTimestamp(message.getTime())
                .setReadPendingIntent(readPendingIntent);
//                .setReplyAction(replyIntent, remoteInput);
//                .addMessage(message.getBody());

    if(sharedpreferences.getBoolean("Auto_reply", false)) {
        unreadConvBuilder.setReplyAction(replyIntent, remoteInput);

        StringBuilder messageForNotification = new StringBuilder();
        Iterator iter = LoginActivity.linkedHashMap.entrySet().iterator();
        messageForNotification.append(message.getBody());
        messageForNotification.append(EOL);
        while (iter.hasNext()) {
        Map.Entry mEntry = (Map.Entry) iter.next();
        messageForNotification.append(" Reply ");
        messageForNotification.append(mEntry.getKey());
        if (iter.hasNext()) {
            messageForNotification.append(EOL);
        }
        }
        unreadConvBuilder.addMessage(messageForNotification.toString());
    }else{
        unreadConvBuilder.setReplyAction(replyIntent, remoteInput);
        replyIntent.cancel();
        unreadConvBuilder.addMessage(message.getBody());
    }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(bitmap)
                .setContentText(message.getBody())
                .setWhen(message.getTime())
                .setContentTitle(message.getName())
                .setContentIntent(readPendingIntent)
                .extend(new CarExtender()
                        .setUnreadConversation(unreadConvBuilder.build())
                        .setColor(getApplicationContext()
                                .getResources().getColor(R.color.default_color_light)));

        mNotificationManager.notify(notification_Id, builder.build());
    }

public Bitmap getBitmapFromURL(String strURL) {
    try {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        URL url = new URL(strURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.connect();
        InputStream input = connection.getInputStream();
        Bitmap myBitmap = BitmapFactory.decodeStream(input);
        return myBitmap;
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}

    public static int nextValue() {
        return counter.getAndIncrement();
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage " + msg);
        if (msg.what == Constants.EventReturnState.NEW_IN_MESSAGE.value){

            MessageModel messageModel = (MessageModel)msg.obj;
            sendNotificationForConversation(messageModel, nextValue());
        }
        return false;
    }
}

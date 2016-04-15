package com.familycircle.auto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class MessageReadReceiver extends BroadcastReceiver {
    private static final String TAG = MessageReadReceiver.class.getSimpleName();

    private static final String CONVERSATION_ID = "conversation_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        int conversationId = intent.getIntExtra(CONVERSATION_ID, -1);
        if (conversationId != -1) {
            Log.d(TAG, "Conversation " + conversationId + " was read");
            NotificationManagerCompat.from(context)
                    .cancel(conversationId);
            String conversation_tn = intent.getStringExtra(MessagingService.CONVERSATION_TN);
            String conversation_name = intent.getStringExtra(MessagingService.CONVERSATION_NAME);
                Intent reply_intent = new Intent(context, MessagingService.class);
                reply_intent.putExtra("read_message", true);
                reply_intent.putExtra(MessagingService.CONVERSATION_TN, conversation_tn);
                reply_intent.putExtra(MessagingService.CONVERSATION_NAME, conversation_name);
                context.startService(reply_intent);
        }
    }
}

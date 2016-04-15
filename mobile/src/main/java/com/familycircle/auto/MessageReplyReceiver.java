package com.familycircle.auto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

/**
 * A receiver that gets called when a reply is sent to a given conversationId
 */
public class MessageReplyReceiver extends BroadcastReceiver {

    private static final String TAG = MessageReplyReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MessagingService.REPLY_ACTION.equals(intent.getAction())) {
//            Toast.makeText(context, "Message Received", Toast.LENGTH_LONG).show();
            int conversationId = intent.getIntExtra(MessagingService.CONVERSATION_ID, -1);
            String conversation_tn = intent.getStringExtra(MessagingService.CONVERSATION_TN);
            String conversation_name = intent.getStringExtra(MessagingService.CONVERSATION_NAME);

            CharSequence reply = getMessageText(intent);

            if (conversationId != -1) {
                Log.d(TAG, "Got reply (" + reply + ") for ConversationId " + conversationId);
                Intent reply_intent = new Intent(context, MessagingService.class);
                reply_intent.putExtra("reply_message", reply);
                reply_intent.putExtra(MessagingService.CONVERSATION_TN, conversation_tn);
                reply_intent.putExtra(MessagingService.CONVERSATION_NAME, conversation_name);
                context.startService(reply_intent);
            }
        }
    }

    /**
     * Get the message text from the intent.
     * Note that you should call {@code RemoteInput#getResultsFromIntent(intent)} to process
     * the RemoteInput.
     */
    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(MessagingService.EXTRA_VOICE_REPLY);
        }
        return null;
    }
}

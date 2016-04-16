package com.familycircle.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.adapters.MessageDetailListAdapter;
import com.familycircle.lib.utils.Logger;
import com.familycircle.manager.PubSubManager;
import com.familycircle.manager.TeamManager;
import com.familycircle.utils.DBInterface;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.NotificationMgr;
import com.familycircle.utils.SmsNotificationMgr;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.TEAMUtils;
import com.familycircle.lib.utils.Foreground;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.lib.utils.db.SmsDbAdapter;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IMessagingChannelClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.MessageModel;
import com.familycircle.utils.network.DoorInsertRequest;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateStreamValue;
import com.familycircle.utils.network.M2XGetAllStreamValues;
import com.familycircle.utils.network.M2XGetStreamValues;
import com.familycircle.utils.network.QueryDbInvite;
import com.familycircle.utils.network.QueryDbUser;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.Types;
import com.familycircle.utils.network.model.DoorCodeModel;
import com.familycircle.utils.network.model.M2XAllValuesModel;
import com.familycircle.utils.network.model.M2XValuesModel;
import com.familycircle.utils.network.model.UserObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageDetailActivity extends ActionBarActivity implements Handler.Callback,
        View.OnClickListener, PubSubManager.OnPubNubMessage, ResponseListener{

    private static final String TAG = "MessageDetailActivity";
    String userTagId;
    private ContactModel contact;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private IMessagingChannelClient messagingChannelClient;
    private Handler mHandler;

    private Button sendButton;
    private EditText etMessage;
    private TextView tvChatemptylist;
    private ListView listView;
    private MessageDetailListAdapter chatAdapter;
    private MessageLoadingTask messageLoadTask = null;

    private boolean isSent = false;
    private static final String ARG_PARAM1 = "mid";
    private static final String ARG_PARAM2 = "unreadCnt";
    //private int mid;
    private int unreadCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        userTagId = intent.getStringExtra("TAG_ID");
        if (userTagId==null||userTagId.isEmpty()){
            finish();
        }

        //mid = intent.getIntExtra(ARG_PARAM1, 0);
        unreadCnt = intent.getIntExtra(ARG_PARAM2, 0);


        setContentView(R.layout.activity_message_detail);

        messagingChannelClient = TeamManager.getInstance().getMessagingClient();
        mHandler = new Handler(this);

        contact = ContactsStaticDataModel.getContactByIdTag(userTagId);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            toolbarTitle = (TextView)toolbar.findViewById(R.id.toolbar_header);
            if (contact!=null) toolbarTitle.setText(contact.getName());
        }

        sendButton = (Button)findViewById(R.id.sendChatMsg);
        sendButton.setOnClickListener(this);
        etMessage = (EditText)findViewById(R.id.chatMessageArea);
        tvChatemptylist = (TextView) findViewById(R.id.chatemptylist);
        listView = (ListView) findViewById(R.id.chatlistview);
        listView.setVisibility(View.VISIBLE);
        if (chatAdapter == null) {
            chatAdapter = new MessageDetailListAdapter(this);
        }

        listView.setAdapter(chatAdapter);


    }

    @Override
    public void onResume(){
        super.onResume();

        SmsNotificationMgr notificationMgr = new SmsNotificationMgr();
        notificationMgr.cancelNotification(TEAMConstants.NOTIFICATION_NEW_SMS_ID);
        TeamManager.getInstance().addMessagingClientHandler(mHandler);

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Warning: Encountered network issue, message may not be delivered", Toast.LENGTH_LONG).show();
            //return;
        } else if (!TeamManager.getInstance().isConnected()){
            Toast.makeText(getApplicationContext(), "Warning: Network Connection to TEAM lost, message cannot be delivered", Toast.LENGTH_LONG).show();
            //return;
        }

        if (messageLoadTask == null) {
            // Load existing messages for user, userTagId
            messageLoadTask = new MessageLoadingTask(userTagId, unreadCnt, isSent);
            messageLoadTask.execute((Void) null);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        TeamManager.getInstance().removeMessagingClientHandler(mHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.message_settings) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void drawChatBubble(final MessageModel messageModel){
        tvChatemptylist.setVisibility(View.GONE);
        ArrayList<MessageModel> list = new ArrayList<MessageModel>();
        list.add(messageModel);
        chatAdapter.addData(list, false);

    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage " + msg);
        if (msg.what == Constants.EventReturnState.NEW_IN_MESSAGE.value){
            MessageModel messageModel = (MessageModel)msg.obj;
            if (messageModel!=null){
                drawChatBubble(messageModel);
                processIncomingChatMessage(messageModel.getBody());
                /*
                ArrayList<MessageModel> list = new ArrayList<MessageModel>();
                list.add(messageModel);
                chatAdapter.addData(list, false);
                */
            } else {
                Log.e(TAG, "NEW_IN_MESSAGE IS NULL");
            }
        }
        if (msg.what == Constants.EventReturnState.MESSAGE_SEND_SUCCESS.value){
            Toast.makeText(getApplicationContext(), "Message sent successfully", Toast.LENGTH_SHORT).show();
        }
        if (msg.what == Constants.EventReturnState.MESSAGE_SEND_ERROR.value){
            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_SHORT).show();
        }
        if (msg.what == Constants.EventReturnState.MESSAGE_SEND_CONNECTION_ERROR.value){
            Toast.makeText(getApplicationContext(), "Could not send message, connection error", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sendChatMsg){

            String message = etMessage.getText().toString();

            if (!NetworkUtils.IsNetworkAvailable(this)){
                Toast.makeText(getApplicationContext(), "Unable to send message, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
                return;
            }

            if (!TeamManager.getInstance().isConnected()){
                Toast.makeText(getApplicationContext(), "Warning: Network Connection to TEAM lost, message cannot be delivered", Toast.LENGTH_LONG).show();
                return;
            }

            if (contact==null){
                Toast.makeText(getApplicationContext(), "Please select a sender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (message==null||message.trim().isEmpty()){
                Toast.makeText(getApplicationContext(), "Please type a message to send", Toast.LENGTH_SHORT).show();

            } else {
                MessageModel messageModel = new MessageModel();
                PrefManagerBase prefMgr = new PrefManagerBase();
                messageModel.setMid(prefMgr.getNextId());
                messageModel.setTn(contact.getIdTag());
                messageModel.setDir("O");
                messageModel.setBody(message);
                processChatMessage(message);
                //messageModel.setServerId(id);
                messageModel.setStatus("R");
                messageModel.setTime((new Date()).getTime());
                messageModel.setName(contact.getName());

                List<MessageModel> messageModels = new ArrayList<MessageModel>();
                messageModels.add(messageModel);

                messagingChannelClient.sendMessage(messageModels);
                //tvChatemptylist.setText("");
                drawChatBubble(messageModel);
                etMessage.setText("");
            }
        }
    }

    private void processIncomingChatMessage(String m){
        try {
            String message = m.toLowerCase().trim();

            MessageModel messageModel = new MessageModel();
            PrefManagerBase prefMgr = new PrefManagerBase();
            messageModel.setMid(prefMgr.getNextId());
            messageModel.setTn(contact.getIdTag());
            messageModel.setDir("O");
            //messageModel.setServerId(id);
            messageModel.setStatus("R");
            messageModel.setTime((new Date()).getTime());
            messageModel.setName(contact.getName());

            if (message.contains("use")
                    && message.contains("door")
                    && message.contains("code")) {

                messageModel.setBody("Helper Bot: Your can use the door code from menu. ");
                List<MessageModel> messageModels = new ArrayList<MessageModel>();
                messageModels.add(messageModel);
                drawChatBubble(messageModel);

            }


        }catch (Exception e){
            Logger.e("Error while processing chat message", e);
        }
    }

    private void processChatMessage(String m){

        try {

            String message = m.toLowerCase().trim();
            int pos = message.indexOf("sos ");
            UserObject userObject = LoginRequest.getUserObject();
            if (pos == 0) {
                Date date = new Date();

                M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue(null, userObject.m2x_id, "panic", "alphanumeric", message +" @ " + date.toString());
                m2XCreateStream.exec();
                PubSubManager.getInstance().startSharingLocation();
            }

            if (message.contains("use")
                    && message.contains("door")
                    && message.contains("code")) {
                DoorCodeModel doorCodeModel = new DoorCodeModel();
                doorCodeModel.code = "1111";
                doorCodeModel.fromUser = userObject.email;
                doorCodeModel.toUser = userTagId;
                try {
                    DoorInsertRequest doorInsertRequest = new DoorInsertRequest(doorCodeModel, this);
                    doorInsertRequest.exec();

                } catch (Exception e) {
                    Logger.e("Door insert error", e);
                }
            }

            if (message.contains("where")
                    && message.contains("are")
                    && message.contains("you")) {
                TeamManager.getInstance().sendCommandMessage("enable_location", userTagId);
                QueryDbUser queryDbUser = new QueryDbUser(userTagId, this);
                queryDbUser.exec();
            }

            if (message.contains("where")
                    && message.contains("r")
                    && message.contains("u")) {
                TeamManager.getInstance().sendCommandMessage("enable_location", userTagId);
                QueryDbUser queryDbUser = new QueryDbUser(userTagId, this);
                queryDbUser.exec();
            }

            if (message.contains("how")
                    && message.contains("r")
                    && message.contains("u")) {
                TeamManager.getInstance().sendCommandMessage("enable_vitals", userTagId);
                QueryDbUser queryDbUser = new QueryDbUser(userTagId, this);
                queryDbUser.exec();
            }

            if (message.contains("how")
                    && message.contains("are")
                    && message.contains("you")) {
                TeamManager.getInstance().sendCommandMessage("enable_vitals", userTagId);
                QueryDbUser queryDbUser = new QueryDbUser(userTagId, this);
                queryDbUser.exec();
            }

            if (message.contains("how")
                    && message.contains("health")) {
                TeamManager.getInstance().sendCommandMessage("enable_vitals", userTagId);
                QueryDbUser queryDbUser = new QueryDbUser(userTagId, this);
                queryDbUser.exec();
            }
        } catch (Exception e){
            Logger.e("Error while processing chat message", e);
        }

    }

    @Override
    public void onPubNubMessage(String channel, Object message) {
        try {

            if (this==null||Foreground.instance.isBackground()) return;

            MessageModel messageModel = new MessageModel();
            PrefManagerBase prefMgr = new PrefManagerBase();
            messageModel.setMid(prefMgr.getNextId());
            messageModel.setTn(contact.getIdTag());
            messageModel.setDir("O");
            //messageModel.setServerId(id);
            messageModel.setStatus("R");
            messageModel.setTime((new Date()).getTime());
            messageModel.setName(contact.getName());

            JSONObject jsonObject = new JSONObject(message.toString());
            String from = jsonObject.getString("from");
            String type = jsonObject.getString("type");

            if (type.equalsIgnoreCase("panic")) {
                String messageValue = jsonObject.getString("value");
                messageModel.setBody("News Flash: Panic from " + from + "=> " + messageValue);
                List<MessageModel> messageModels = new ArrayList<MessageModel>();
                messageModels.add(messageModel);
                drawChatBubble(messageModel);
            }

            if (type.equalsIgnoreCase("heartbeat")
                    || type.equalsIgnoreCase("heartrate")) {
                String messageValue = jsonObject.getString("value");
                messageModel.setBody("News Flash: Abormal heart beat from " + from + "=> Beat: " + messageValue);
                List<MessageModel> messageModels = new ArrayList<MessageModel>();
                messageModels.add(messageModel);
                drawChatBubble(messageModel);
            }


            if (type.equalsIgnoreCase("distance_stream")
                    || type.equalsIgnoreCase("door distance")) {
                String messageValue = jsonObject.getString("value");
                messageModel.setBody("News Flash: Alert => Someone is " + messageValue + " from your door.");
                List<MessageModel> messageModels = new ArrayList<MessageModel>();
                messageModels.add(messageModel);
                drawChatBubble(messageModel);
            }



        } catch (Exception e){
            Logger.e("Exception in chat window", e);
        }
    }

    @Override
    public void onConnect(String channel, Object message) {


    }

    @Override
    public void onSuccess(Response response) {
            if (response.getRequestType() == Types.RequestType.QUERY_DB_USER){
                UserObject userObject = (UserObject)response.getModel();
                Logger.d("db user from " );
                if (userObject==null) return;
                Logger.d("db user from " + userObject.email);
                M2XGetStreamValues m2XGetAllStreamValues = new M2XGetStreamValues(this, userObject.m2x_id, "location",1);
                m2XGetAllStreamValues.exec();
                M2XGetStreamValues m2XGetAllStreamValues2 = new M2XGetStreamValues(this, userObject.m2x_id, "heartbeat",1);
                m2XGetAllStreamValues2.exec();
                M2XGetStreamValues m2XGetAllStreamValues3 = new M2XGetStreamValues(this, userObject.m2x_id, "distance",1);
                m2XGetAllStreamValues3.exec();
            }

        if (response.getRequestType() == Types.RequestType.QUERY_INSERT_DOOR_CODE){
            Toast.makeText(getApplicationContext(), "Door code sent", Toast.LENGTH_LONG).show();
        }


            if (response.getRequestType()==Types.RequestType.M2X_GET_STREAM){

                String messageLabel = "";

                M2XValuesModel m2XAllValuesModel = (M2XValuesModel)response.getModel();
                if (m2XAllValuesModel!=null){
                    List<M2XValuesModel.ValueModel>  m2xValues = m2XAllValuesModel.m2xValues;
                    if (m2xValues!=null){
                        for (M2XValuesModel.ValueModel valuesModel: m2xValues){
                            if (valuesModel!=null){
                                if (valuesModel.value!=null && valuesModel.value.contains("long:")) {
                                    messageLabel = "Bot Helper: => Last Location Details for " + userTagId +"\n ";
                                    messageLabel = messageLabel + valuesModel.timestamp + " \n: " + valuesModel.value + " \n";
                                } else if (valuesModel.value!=null && valuesModel.value.contains("motion")) {
                                    messageLabel = "Bot Helper: => Last Motion Details for " + userTagId +"\n ";
                                    messageLabel = messageLabel + valuesModel.timestamp + " \n: " + valuesModel.value + " \n";
                                } else {
                                    messageLabel = "Bot Helper: => Last Heartbeat Details for " + userTagId +"\n ";
                                    messageLabel = messageLabel + valuesModel.timestamp + " \n HeartBeat : " + valuesModel.value + " BPM \n";
                                }

                            }
                        }
                    }
                }

                if (messageLabel!=null){

                    MessageModel messageModel = new MessageModel();
                    PrefManagerBase prefMgr = new PrefManagerBase();
                    messageModel.setMid(prefMgr.getNextId());
                    messageModel.setTn(contact.getIdTag());
                    messageModel.setDir("O");
                    //messageModel.setServerId(id);
                    messageModel.setStatus("R");
                    messageModel.setTime((new Date()).getTime());
                    messageModel.setName(contact.getName());

                    messageModel.setBody(messageLabel);
                    List<MessageModel> messageModels = new ArrayList<MessageModel>();
                    messageModels.add(messageModel);
                    drawChatBubble(messageModel);

                }
            }
    }

    @Override
    public void onFailure(Response response) {
        Logger.e("Failed from " + response.getRequestType());
    }

    public class MessageLoadingTask extends AsyncTask<Void, Void, Boolean> {

        private List<MessageModel> messages;
        private String _tn;
        private int _unreadCnt=0;
        private boolean _isSent=false;

        public MessageLoadingTask(String tn, int unreadCnt, boolean isSent) {
            if (messages != null) {
                messages.clear();
                messages = null;
            }
            _tn = tn;
            _unreadCnt = unreadCnt;
            _isSent = isSent;
        }

        @Override
        protected void onPreExecute() {
            try {


            } catch (Exception e) {

            }

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (_tn == null || _tn.trim().isEmpty()
                        || _tn.trim().equals("null"))
                    return true;
                SmsDbAdapter dbAdapter = new SmsDbAdapter();
                messages = dbAdapter.getMessagesByTn(_tn);
                if (messages == null) {
                    Log.d(TAG, "MessageLoadingTask Detail is null " + _tn);
                } else {
                    Log.d(TAG, "MessageLoadingTask Detail # " + messages.size()
                            + "," + _tn);
                }
                if (_unreadCnt > 0) {
                    dbAdapter.updateMessageStatusByTn(_tn, "R");
                    TEAMUtils.updateSmsAppBadge();
                }
                return true;

            } catch (Exception e) {
                Log.e(TAG, "doinbackground " + e.toString(), e);
            } catch (Error e) {
                Log.e(TAG, "doinbackground " + e.toString(), e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (this == null || Foreground.instance.isBackground())
                return;
            try {
                // showProgress(false, null, null);
                if (success) {
                    if (messages != null) {
                        if (messages.size()>0){
                            tvChatemptylist.setVisibility(View.GONE);
                        } else {
                            tvChatemptylist.setVisibility(View.VISIBLE);
                        }
                        chatAdapter.loadData(messages, _isSent);
                    } else {
                        tvChatemptylist.setVisibility(View.VISIBLE);
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "onPostExecute " + e.toString(), e);
            } finally {
                messageLoadTask = null;
            }
        }

        @Override
        protected void onCancelled() {
            messageLoadTask = null;
            // showProgress(false, null, null);
        }
    }
}

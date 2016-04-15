package com.familycircle.lib.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.familycircle.lib.utils.db.SmsDbAdapter;
import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.sdk.ChannelManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IMessagingChannelClient;
import com.familycircle.sdk.IMessagingChannelEvent;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.MessageModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by samratsen on 5/28/15.
 */
public class MessagingClient implements IMessagingChannelClient {

    private List<IMessagingChannelEvent> _eventListeners = new ArrayList<IMessagingChannelEvent>();

    private HandlerThread workerThread;
    private Handler workerHandler;

    public MessagingClient(){
        workerThread = new HandlerThread("MessagingClient Thread");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
    }

    public void disposeWorkerThread(){
        workerThread.getLooper().quit();
    }

    @Override
    public void sendMessage(final List<MessageModel> messageModels) {

        if (!ChannelManager.getInstance().isConnected()){
            updateEventListeners(Constants.EventReturnState.MESSAGE_SEND_CONNECTION_ERROR, null, "Not Connected", null);
            return;
        }

        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                ContactModel loginUser = ContactsStaticDataModel.getLogInUser();

                for (MessageModel message:messageModels) {


                    //String jsonStr = "{\"type\":\"message\", \"to\":[\"" + message.getTn() + "\"], \"from\":\"" + loginUser.getIdTag() + "\", \"message\":\"" + message.getBody() + "\"}";

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("type", "message");
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(message.getTn());
                        jsonObject.put("to", jsonArray);
                        jsonObject.put("from", loginUser.getIdTag());
                        jsonObject.put("message", message.getBody());
                        //String jsonStr = jsonObject.toString();
                        PnRTCManager.getInstance().transmitMessage(message.getTn(), jsonObject);

                        SmsDbAdapter dbAdapter = new SmsDbAdapter();
                        dbAdapter.insertSmsLog(message);
                        updateEventListeners(Constants.EventReturnState.MESSAGE_SEND_SUCCESS, loginUser, "Message sent successfully", null);

                    } catch (Exception e) {

                        SmsDbAdapter dbAdapter = new SmsDbAdapter();
                        message.setStatus("E");
                        dbAdapter.insertSmsLog(message);
                        updateEventListeners(Constants.EventReturnState.MESSAGE_SEND_ERROR, loginUser, "Message could not be sent " + e.toString(), null);
                    }
                }
            }
        });

    }

    @Override
    public List<MessageModel> getMessageHistory(List<ContactModel> contactModelList) {
        List<MessageModel> returnMessages = null;

        SmsDbAdapter dbAdapter = new SmsDbAdapter();

        if (contactModelList==null||contactModelList.isEmpty()){
            returnMessages = dbAdapter.getGroupedMessage();

        } else {
            if (contactModelList.size()==1){
                returnMessages = dbAdapter.getMessagesByTn(contactModelList.get(0).getIdTag());
            } else {
                String [] tnArray =  new String[contactModelList.size()];
                int cnt=0;
                for (ContactModel contact:contactModelList) {
                    tnArray[cnt++] = contact.getIdTag();
                }
                returnMessages = dbAdapter.getMessagesByMultipleTn(tnArray);
            }
        }

        return returnMessages;
    }

    public void addMessagingEventListener(IMessagingChannelEvent eventListener){

        boolean found = false;
        for (IMessagingChannelEvent channelEvent:_eventListeners){
            if (eventListener == channelEvent) {
                found = true;
                break;
            }

        }
        if (!found){
            _eventListeners.add(eventListener);
        }
    }

    public void removeMessagingEventListener(IMessagingChannelEvent eventListener){
        //_eventListeners.remove(eventListener); double registration will cause issues
        if (_eventListeners==null||_eventListeners.isEmpty()) return;
        Iterator<IMessagingChannelEvent> itr = _eventListeners.iterator();
        while(itr.hasNext()){
            IMessagingChannelEvent _event = itr.next();
            if (_event!=null && _event == eventListener){
                itr.remove();
            }
        }
    }

    public void updateEventListeners(final Constants.EventReturnState returnState, final ContactModel userContactModel, final String msg, final Object extra){
        if (_eventListeners==null||_eventListeners.isEmpty()) return;

        MessageModel messageModel = null;
        if (returnState == Constants.EventReturnState.NEW_IN_MESSAGE){

            messageModel = new MessageModel();
            PrefManagerBase prefMgr = new PrefManagerBase();
            messageModel.setMid(prefMgr.getNextId());
            String extraString = (String)extra;
            String [] extraArray = extraString.split(",");
            String fromUser = extraArray[0];
            String id = extraArray[1];

            messageModel.setTn(fromUser);
            messageModel.setDir("I");
            String msgStr = msg;
            try {
                JSONObject jsonObject = new JSONObject(msg);
                if (!jsonObject.isNull("message")){
                    msgStr = jsonObject.getString("message");
                }
            } catch (JSONException e) {
                Log.e("MessagingClient", e.toString(), e);
            }
            messageModel.setBody(msgStr);
            messageModel.setServerId(id);
            messageModel.setStatus("U");
            long t = Long.parseLong(extraArray[2]);
            messageModel.setTime(t);
            ContactModel fromModel = ContactsStaticDataModel.getContactByIdTag(fromUser);
            messageModel.setName(fromModel.getName());

            SmsDbAdapter dbAdapter = new SmsDbAdapter();
            dbAdapter.insertSmsLog(messageModel);

        }

        for (IMessagingChannelEvent channelEvent:_eventListeners){
            channelEvent.onMessagingEvent(returnState, userContactModel, msg, messageModel!=null?messageModel:extra);

        }
    }
}

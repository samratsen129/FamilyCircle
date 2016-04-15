package com.familycircle.sdk;

import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.MessageModel;

import java.util.List;

/**
 * Created by samratsen on 5/28/15.
 */
public interface IMessagingChannelClient {
    /**
     * Method to send message to users
     * Developers should use the IMessagingChannelEvent to receive feedaback
     * @param messageModels - user messages to be sent
     */
    public void sendMessage(final List<MessageModel> messageModels);

    /**
     * Method returns the list of messages details
     * Developers should use the IMessagingChannelEvent to receive feedaback
     * @param contactModelList - if empty it will enter the last message conversation from each user
     * @return
     */
    public List<MessageModel> getMessageHistory (List<ContactModel> contactModelList);

}

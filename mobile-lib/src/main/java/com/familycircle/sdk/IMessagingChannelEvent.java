package com.familycircle.sdk;

import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 5/28/15.
 *
 */
public interface IMessagingChannelEvent {
    /**
     * Developers should use the this to receive feedback
     * Especially helpful for receiving incoming messages
     * @param returnState
     * @param userContactModel
     * @param msg
     * @param extra
     */
    public void onMessagingEvent(final Constants.EventReturnState returnState, final ContactModel userContactModel, final String msg, final Object extra);
}

package com.familycircle.sdk;

import com.familycircle.sdk.models.CallModel;
import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 6/10/15.
 */
public interface IAVCallChannelEvent {
    /**
     * Developers should use the this to receive feedback
     * Especially helpful for receiving incoming webrtc messages
     * @param returnState
     * @param callModel
     * @param msg
     * @param extra
     */
    public void onMessagingEvent(final Constants.EventReturnState returnState, final CallModel callModel, final String msg, final Object extra);
}

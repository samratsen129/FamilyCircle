package com.familycircle.sdk;

import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 5/28/15.
 */
public interface IControlChannelEvent {
    public void onControlEvent(final Constants.EventReturnState returnState, final ContactModel userContactModel, final String msg, final Object extra);
}

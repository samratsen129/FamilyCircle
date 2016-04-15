package com.familycircle.manager;

import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 6/5/15.
 */
public class ChannelManagerController  extends BaseController {
    @Override
    public boolean handleMessage(int what, final ContactModel userContactModel, final String msg, final Object extra) {
        notifyHandlers(
                what,
                0,
                0, extra);
        return false;
    }
}

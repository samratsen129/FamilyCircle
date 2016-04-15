package com.familycircle.manager;

import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 6/10/15.
 */
public class WebRtcClientController extends BaseController {
    @Override
    public boolean handleMessage(int what, final ContactModel userContactModel, final String msg, final Object extra) {

        //if (what != Constants.EventReturnState.GET_USERS_SUCCESS.value){
        notifyHandlers(
                what,
                0,
                0, extra);
        return false;
    }
}

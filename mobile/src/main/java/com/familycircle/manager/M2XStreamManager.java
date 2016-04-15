package com.familycircle.manager;

import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateHRTrigger;
import com.familycircle.utils.network.M2XCreateLocationTrigger;
import com.familycircle.utils.network.M2XCreateStream;
import com.familycircle.utils.network.M2XCreateTemperatureTrigger;
import com.familycircle.utils.network.M2XGetTrigger;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.Types;
import com.familycircle.utils.network.model.M2XTriggerModel;
import com.familycircle.utils.network.model.UserObject;

import java.util.List;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XStreamManager implements ResponseListener {

    private ResponseListener responseCallback;
    private boolean isLocCompleted=false, isTempCompleted=false, isHRCompleted=false, isVitalsCompleted, isPanicCompleted;
    private int TEMPERATURE_LIMIT = 90;
    private int HR_LIMIT = 80;

    public void checkAndCreateTriggers(ResponseListener responseCallback){

        this.responseCallback = responseCallback;
        UserObject userObject = LoginRequest.getUserObject();
        M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "location", "degree", "deg", "alphanumeric");
        m2XCreateStream.exec();
        isLocCompleted=true;
    }

    @Override
    public void onSuccess(Response response) {
        if (response.getRequestType() == Types.RequestType.M2X_CREATE_STREAM){
            if (!evaluateMore()){
                if (responseCallback!=null){
                    response.setRequestType(Types.RequestType.M2X_CREATE_STREAM_MGR);
                    responseCallback.onSuccess(response);
                }
            }
        }
    }

    private boolean evaluateMore() {
        UserObject userObject = LoginRequest.getUserObject();
        if (!isTempCompleted) {
            M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "temperature", "degree", "deg", "numeric");
            m2XCreateStream.exec();
            isTempCompleted = true;
            return true;
        }
        if (!isHRCompleted) {
            M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "heartbeat", "beats", "beats", "numeric");
            m2XCreateStream.exec();
            isHRCompleted = true;
            return true;
        }
        if (!isVitalsCompleted) {
            M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "distance", "meters", "m", "alphanumeric");
            m2XCreateStream.exec();
            isVitalsCompleted = true;
            return true;
        }
        if (!isPanicCompleted) {
            M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "panic", "none", "none", "alphanumeric");
            m2XCreateStream.exec();
            isPanicCompleted = true;
            return true;
        }
        if (!isLocCompleted) {
            M2XCreateStream m2XCreateStream = new M2XCreateStream(this, userObject.m2x_id, "panic", "none", "none", "alphanumeric");
            m2XCreateStream.exec();
            isLocCompleted = true;
            return true;
        }
        return false;
    }

    @Override
    public void onFailure(Response response) {
        if (responseCallback!=null){
            response.setRequestType(Types.RequestType.M2X_CREATE_STREAM_MGR);
            responseCallback.onFailure(response);
        }
    }
}

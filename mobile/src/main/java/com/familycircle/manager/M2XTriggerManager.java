package com.familycircle.manager;

import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateHRTrigger;
import com.familycircle.utils.network.M2XCreateLocationTrigger;
import com.familycircle.utils.network.M2XCreatePanicTrigger;
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
public class M2XTriggerManager implements ResponseListener {

    private ResponseListener responseCallback;
    private boolean isLocCompleted=false, isTempCompleted=false, isHRCompleted=false, isPanicCompleted=false;
    private int TEMPERATURE_LIMIT = 90;
    private int HR_LIMIT = 80;

    public void checkAndCreateTriggers(ResponseListener responseCallback){

        this.responseCallback = responseCallback;
        UserObject userObject = LoginRequest.getUserObject();
        M2XGetTrigger m2XGetTrigger = new M2XGetTrigger(this, userObject.device_id);
        m2XGetTrigger.exec();
    }

    @Override
    public void onSuccess(Response response) {
        if (response.getRequestType() == Types.RequestType.M2X_GET_TRIGGERS){
            M2XTriggerModel m2XTriggerModel = (M2XTriggerModel)response.getModel();
            if (m2XTriggerModel !=null) {
                List<M2XTriggerModel.Trigger> triggers = m2XTriggerModel.triggerList;
                if (triggers!=null && !triggers.isEmpty()){
                    for (M2XTriggerModel.Trigger trigger:triggers){
                        String id = trigger.id;
                        String name = trigger.name;
                        if (name.equalsIgnoreCase("temperature")){
                            isTempCompleted=true;
                        } else if (name.equalsIgnoreCase("heartrate")){
                            isHRCompleted =true;
                        } else if (name.equalsIgnoreCase("location")){
                            isLocCompleted = true;
                        } else if (name.equalsIgnoreCase("panic")){
                            isPanicCompleted = true;
                        }
                    }
                }
            }

            if (!evaluateMore()){
                if (responseCallback!=null){
                    responseCallback.onSuccess(response);
                }
            }

        } else if (response.getRequestType() == Types.RequestType.M2X_CREATE_TRIGGERS){
            if (!evaluateMore()){
                if (responseCallback!=null){
                    responseCallback.onSuccess(response);
                }
            }
        }
    }

    private boolean evaluateMore() {
        UserObject userObject = LoginRequest.getUserObject();
        if (!isTempCompleted) {
            M2XCreateTemperatureTrigger m2XCreateTemperatureTrigger =
                    new M2XCreateTemperatureTrigger(this, userObject.device_id, TEMPERATURE_LIMIT, "temperature", userObject.family_id);
            m2XCreateTemperatureTrigger.exec();
            isTempCompleted = true;
            return true;
        }
        if (!isHRCompleted) {
            M2XCreateHRTrigger m2XCreateHRTrigger =
                    new M2XCreateHRTrigger(this, userObject.device_id, HR_LIMIT, "heartrate", userObject.family_id);
            m2XCreateHRTrigger.exec();
            isHRCompleted = true;
            return true;
        }
        if (!isLocCompleted) {
            M2XCreateLocationTrigger m2XCreateLocationTrigger = new M2XCreateLocationTrigger(this, userObject.device_id, "location", userObject.family_id);
            m2XCreateLocationTrigger.exec();
            isLocCompleted = true;
            return true;
        }
        if (!isPanicCompleted){
            M2XCreatePanicTrigger m2XCreatePanicTrigger = new M2XCreatePanicTrigger(this, userObject.device_id, "panic", userObject.family_id);
            m2XCreatePanicTrigger.exec();
        }
        return false;
    }

    @Override
    public void onFailure(Response response) {
        if (responseCallback!=null){
            responseCallback.onFailure(response);
        }
    }
}

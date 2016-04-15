package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.model.M2XTriggerModel;
import com.familycircle.utils.network.model.UserObject;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XCreatePanicTrigger extends Request {

    private String deviceId, familyId;
    private String triggerName, message;
    private String M2X_CALLBACK = "http://50.116.46.145:5000/m2x-trigger";

    public M2XCreatePanicTrigger(final ResponseListener responseListener, final String deviceId, final String triggerName, final String familyId) {

        super(Types.HttpRequestType.POST, "application/json", "https://api-m2x.att.com/v2/devices/" + deviceId +"/triggers");

        ArrayList data = new ArrayList<NameValuePair>();

        this.deviceId  = deviceId;
        this.familyId = familyId;
        this.triggerName = triggerName;
        this.message = message;

        data.add(new BasicNameValuePair("X-M2X-KEY", TEAMConstants.M2X_KEY));

        setHeaderParams(data);

        this.responseListener = responseListener;

    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response resp = new Response();
        resp.setReponseCode(responseCode);
        resp.setRequestType(getRequestType());

        try {
            resp.setResponseType(Types.NetworkResponseType.SUCCESS);
            JSONObject jsonObject = new JSONObject(response);

            String id = jsonObject.getString("id");
            String name = jsonObject.getString("name");

            M2XTriggerModel m2XTriggerModel = new M2XTriggerModel();

            M2XTriggerModel.Trigger trigger = new M2XTriggerModel.Trigger();
            trigger.id = id;
            trigger.name = name;

            m2XTriggerModel.triggerList.add(trigger);

            resp.setModel(m2XTriggerModel);

            return resp;


        } catch (Exception ex){
            Logger.e("Exception while making request", ex);
        }

        resp.setResponseType(Types.NetworkResponseType.FAILURE);

        return resp;
    }

    @Override
    public List<NameValuePair> getBodyContent() {
        return null;
    }

    @Override
    public String getStringBodyContent() {
        UserObject userObject = LoginRequest.getUserObject();

        String jsonStr = "{ \"name\": \""+triggerName+"\","+
                "  \"conditions\": {"+
                "    \"panic\": { \"changed\": true}"+
                "  },"+
                "  \"frequency\": \"periodic\","+
                "  \"interval\": \"30\","+
                "  \"callback_url\": \""+M2X_CALLBACK+"\","+
                "  \"custom_data\": \"{ \\\"from\\\": \\\""+userObject.email+"\\\", \\\"queues\\\":[\\\""+familyId+"\\\"] }\" "+
                "}";
        return jsonStr;
    }

    @Override
    public boolean clearCookies() {
        return false;
    }

    @Override
    public boolean urlRedirectionRequired() {
        return false;
    }

    @Override
    public boolean useSessionCookies() {
        return false;
    }

    @Override
    public Types.RequestType getRequestType() {
        return Types.RequestType.M2X_CREATE_TRIGGERS;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

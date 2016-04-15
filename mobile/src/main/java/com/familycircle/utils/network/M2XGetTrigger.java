package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.model.M2XTriggerModel;
import com.familycircle.utils.network.model.M2XValuesModel;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XGetTrigger extends Request {

    private String deviceId, streamName;

    public M2XGetTrigger(final ResponseListener responseListener, final String deviceId) {

        super(Types.HttpRequestType.GET, "application/json", "https://api-m2x.att.com/v2/devices/"+deviceId+"/triggers");

        ArrayList data = new ArrayList<NameValuePair>();

        this.deviceId  = deviceId;
        this.streamName = streamName;

        data.add(new BasicNameValuePair("X-M2X-KEY", TEAMConstants.M2X_KEY));

        setHeaderParams(data);

        this.responseListener = responseListener;

    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response resp= new Response();
        resp.setReponseCode(responseCode);
        resp.setRequestType(getRequestType());

        if (responseCode == 200){
            resp.setResponseType(Types.NetworkResponseType.SUCCESS);

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("triggers");

                M2XTriggerModel m2XTriggerModel = new M2XTriggerModel();

                if (jsonArray!=null && jsonArray.length()>0){

                    for (int i=0;i<jsonArray.length();i++) {

                        M2XTriggerModel.Trigger trigger = new M2XTriggerModel.Trigger();

                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);

                        String id = jsonObject1.optString("id");
                        trigger.id = id;
                        String name = jsonObject1.optString("name");
                        trigger.name = name;
                        m2XTriggerModel.triggerList.add(trigger);

                    }
                }
                resp.setModel(m2XTriggerModel);

                return resp;

            } catch (JSONException e) {
                Logger.e("Error while parsing json in m2x get stream", e);

            }

        } else {
            resp.setResponseType(Types.NetworkResponseType.FAILURE);
        }

        return resp;
    }

    @Override
    public List<NameValuePair> getBodyContent() {
        return null;
    }

    @Override
    public String getStringBodyContent() {
        return null;
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
        return Types.RequestType.M2X_GET_TRIGGERS;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

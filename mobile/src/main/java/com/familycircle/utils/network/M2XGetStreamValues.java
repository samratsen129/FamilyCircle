package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.TEAMConstants;
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
 * Created by samratsen on 4/11/16.
 */
public class M2XGetStreamValues extends Request {

    private String deviceId, streamName;

    public M2XGetStreamValues(final ResponseListener responseListener, final String deviceId, final String streamName, int limit) {

        super(Types.HttpRequestType.GET, "application/json", "https://api-m2x.att.com/v2/devices/"+deviceId+"/streams/"+streamName+"/values?limit="+limit);

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

        M2XValuesModel m2XValuesModel = new M2XValuesModel();

        if (responseCode == 200){
            resp.setResponseType(Types.NetworkResponseType.SUCCESS);

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("values");
                if (jsonArray!=null && jsonArray.length()>0){
                    for (int i=0;i<jsonArray.length();i++) {
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        String timestamp = jsonObject1.optString("timestamp");
                        String value = jsonObject1.optString("value");
                        M2XValuesModel.ValueModel valueModel = new M2XValuesModel.ValueModel();
                        valueModel.timestamp = timestamp;
                        valueModel.value = value;
                        m2XValuesModel.m2xValues.add(valueModel);
                    }
                }
                resp.setModel(m2XValuesModel);

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
        return Types.RequestType.M2X_GET_STREAM;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

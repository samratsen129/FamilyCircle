package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.model.M2XAllValuesModel;
import com.familycircle.utils.network.model.M2XValuesModel;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by samratsen on 4/11/16.
 */
public class M2XGetAllStreamValues extends Request {

    private String deviceId, streamName;

    public M2XGetAllStreamValues(final ResponseListener responseListener, final String deviceId, final String streamName, int limit) {

        super(Types.HttpRequestType.GET, "application/json", "https://api-m2x.att.com/v2/devices/"+deviceId+"/values?limit="+limit);

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

        M2XAllValuesModel m2XValuesModel = new M2XAllValuesModel();

        if (responseCode == 200){
            resp.setResponseType(Types.NetworkResponseType.SUCCESS);

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("values");

                if (jsonArray!=null && jsonArray.length()>0){
                    for (int i=0;i<jsonArray.length();i++) {

                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        String timestamp = jsonObject1.optString("timestamp");

                        JSONObject jsonObject2 = jsonObject1.optJSONObject("values");
                        M2XAllValuesModel.ValuesModel valuesModel = new M2XAllValuesModel.ValuesModel();
                        valuesModel.timestamp = timestamp;

                        if (jsonObject2!=null) {

                            Iterator iter = jsonObject2.keys();
                            if (iter!=null) {
                                while (iter.hasNext()) {
                                    String key = (String) iter.next();
                                    String value = jsonObject2.getString(key);
                                    valuesModel.valueMap.put(key, value);
                                }
                            }
                        }

                        m2XValuesModel.m2xValues.add(valuesModel);
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
        return Types.RequestType.M2X_GET_ALL_STREAM;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

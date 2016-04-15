package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.model.M2XDevice;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/11/16.
 */
public class M2XCreateRequest extends Request {

    private String deviceId, familyId, deviceDesc;

    public M2XCreateRequest (final ResponseListener responseListener, final String deviceId, final String familyId, final String deviceDesc) {

        super(Types.HttpRequestType.POST, "application/json", "https://api-m2x.att.com/v2/devices");

        ArrayList data = new ArrayList<NameValuePair>();

        this.deviceId  = deviceId;
        this.familyId = familyId;
        this.deviceDesc = deviceDesc;

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
                M2XDevice m2XDevice = new M2XDevice();
                m2XDevice.id = id;
                resp.setModel(m2XDevice);
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
        String jsonStr = "{ \"name\": \""+deviceId+"\", "+
        "\"description\": \""+deviceDesc+"\","+
                "\"tags\": \""+ familyId+ "\","+
                "\"visibility\": \"private\" }";
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
        return Types.RequestType.M2X_CREATE_DEVICE;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

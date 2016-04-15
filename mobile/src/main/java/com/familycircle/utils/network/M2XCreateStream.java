package com.familycircle.utils.network;

import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.Request;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.Types;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/11/16.
 */
public class M2XCreateStream extends Request {

    private String deviceId, streamName;
    private String label, symbol, type;

    public M2XCreateStream (final ResponseListener responseListener, final String deviceId, final String streamName, String label, String symbol, String type) {

        super(Types.HttpRequestType.PUT, "application/json", "https://api-m2x.att.com/v2/devices/"+deviceId+"/streams/" + streamName);

        ArrayList data = new ArrayList<NameValuePair>();

        this.deviceId  = deviceId;
        this.streamName = streamName;
        this.label = label;
        this.symbol = symbol ;
        this.type = type;

        data.add(new BasicNameValuePair("X-M2X-KEY", TEAMConstants.M2X_KEY));

        setHeaderParams(data);

        this.responseListener = responseListener;

    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response resp= new Response();
        resp.setReponseCode(responseCode);
        resp.setRequestType(getRequestType());
        if (responseCode == 204 || responseCode == 201){
            resp.setResponseType(Types.NetworkResponseType.SUCCESS);
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
        String jsonStr = "{ \"unit\": { \"label\": \""+label+"\", \"symbol\": \""+symbol+"\" }, \"type\":\""+type+"\" }";
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
        return Types.RequestType.M2X_CREATE_STREAM;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}

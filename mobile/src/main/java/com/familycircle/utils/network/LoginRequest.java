package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.DBInterface;
import com.familycircle.utils.network.model.UserObject;

import org.apache.http.Header;
import org.apache.http.NameValuePair;

import java.util.List;

public class LoginRequest extends Request {

    private static UserObject userObject;

    public LoginRequest(final UserObject userObject, final ResponseListener responseListener) {
        super(Types.DatabaseRequestType.QUERY);
        this.userObject = userObject;
        this.responseListener = responseListener;
    }

    public static UserObject getUserObject(){
        return userObject;
    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response responseParse = new Response();

        try {
            boolean result = DBInterface.insertUserDevice(userObject, true);

            if (result){
                responseParse.setResponseType(Types.NetworkResponseType.SUCCESS);
                responseParse.setReponseCode(200);
                return responseParse;
            }else {

                result = DBInterface.updateUserDevice(userObject);
                if (result){
                    responseParse.setResponseType(Types.NetworkResponseType.SUCCESS);
                    responseParse.setReponseCode(200);
                    return responseParse;
                }

                responseParse.setReponseCode(204);
            }

        } catch (Exception e){
            responseParse.setErrorMessage("Error while retrieving user" + e.toString());
            Logger.e("Error while retrieving user.", e);

        }

        responseParse.setResponseType(Types.NetworkResponseType.FAILURE);
        return responseParse;
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
        return Types.RequestType.GET_LOGIN;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}


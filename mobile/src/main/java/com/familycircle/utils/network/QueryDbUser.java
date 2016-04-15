package com.familycircle.utils.network;


import com.familycircle.lib.utils.Logger;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.DBInterface;
import com.familycircle.utils.network.model.UserObject;

import org.apache.http.Header;
import org.apache.http.NameValuePair;

import java.util.List;


public class QueryDbUser extends Request {

    private final String username;

    public QueryDbUser(final String username, final ResponseListener responseListener) {
        super(Types.DatabaseRequestType.QUERY);
        this.username = username;
        this.responseListener = responseListener;
    }

    public String getUsername() {
        return username;
    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response responseParse = new Response();

        try {
            UserObject  userModel = DBInterface.getUserDeviceObject(username);

            if (userModel!=null){
                ContactModel contactModel = ContactsStaticDataModel.getLogInUser();

                responseParse.setModel(userModel);
                responseParse.setResponseType(Types.NetworkResponseType.SUCCESS);
                responseParse.setReponseCode(200);

                return responseParse;
            }else {
                responseParse.setResponseType(Types.NetworkResponseType.SUCCESS);
                responseParse.setReponseCode(204);
                return responseParse;
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
        return Types.RequestType.QUERY_DB_USER;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}


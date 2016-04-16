package com.familycircle.utils.network;

import com.familycircle.lib.utils.Logger;
import com.familycircle.utils.DBInterface;
import com.familycircle.utils.network.model.DoorCodeModel;
import com.familycircle.utils.network.model.InviteModel;

import org.apache.http.Header;
import org.apache.http.NameValuePair;

import java.util.List;

/**
 * Created by sensa1x on 4/14/2016.
 */
public class DoorInsertRequest extends Request {

    private final DoorCodeModel doorCodeModel;

    public DoorInsertRequest(final DoorCodeModel doorCodeModel, final ResponseListener responseListener) {
        super(Types.DatabaseRequestType.QUERY);
        this.doorCodeModel = doorCodeModel;
        this.responseListener = responseListener;
    }

    @Override
    protected Response parse(int responseCode, String response, Header[] headers) {
        Response responseParse = new Response();

        try {

            boolean result = DBInterface.insertDoorCode(doorCodeModel);

            if (result){
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
        return Types.RequestType.QUERY_INSERT_DOOR_CODE;
    }

    @Override
    public boolean cacheResponse(Response responseObject) {
        return false;
    }
}


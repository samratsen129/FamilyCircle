package com.familycircle.sdk;

import com.familycircle.sdk.models.ContactModel;

/**
 * Created by samratsen on 6/5/15.
 */
public interface IChannelManagerClient {
    public void connectControlChannel(ContactModel contactModel);
    public void logOff();
    public void removeControlChannel(ContactModel contactModel);
    public void requestAllUsers();
    public void requestCreateNewUser(ContactModel contactModel);
    public boolean isConnected();
}

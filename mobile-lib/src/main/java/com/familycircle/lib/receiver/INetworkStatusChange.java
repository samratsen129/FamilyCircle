package com.familycircle.lib.receiver;

import com.familycircle.sdk.Constants;

/**
 * Created by samratsen on 5/30/15.
 */
public interface INetworkStatusChange {
    public void onNetworkStateChange(boolean isNetworkConnected, Constants.NetworkType type);
}

package com.familycircle.lib.utils;

/**
 * Created by samratsen on 2/27/15.
 */
public interface IMediaStatusListener {
    public void onMediaReadyListner();

    public void onCompleteListner();

    public void onMediaErrorListner();

    public void onMediaPausedListner();
}

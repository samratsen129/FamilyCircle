package com.familycircle.lib.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.familycircle.sdk.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 5/30/15.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private final static List<INetworkStatusChange> objectList =
            new ArrayList<INetworkStatusChange>();

    public static void addForNetworkStateListener(INetworkStatusChange network)
    {
        objectList.add(network);
    }

    public static void removeFromNetworkListener(INetworkStatusChange network)
    {
        objectList.remove(network);
    }

    public static void emptyNetworkListener()
    {
        objectList.clear();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        networkStatus(context);
    }

    private void networkStatus(Context context)
    {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        Constants.NetworkType type = Constants.NetworkType.NULL;

        if (cm != null)
        {

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null)
            {
                boolean isConnected = activeNetwork.isConnected();
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                boolean isMobile = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
                if (isWiFi)
                {
                    type = Constants.NetworkType.WIFI;
                }
                else if (isMobile)
                {
                    type = Constants.NetworkType.MOBILE;
                }
                else
                {
                    type = Constants.NetworkType.NULL;
                }

                if (!objectList.isEmpty())
                {
                    for (INetworkStatusChange network : objectList)
                    {
                        network.onNetworkStateChange(isConnected, type);
                    }

                }


            }
            else
            {
                if (!objectList.isEmpty())
                {
                    for (INetworkStatusChange network : objectList)
                    {
                        network.onNetworkStateChange(false, type);
                    }
                }
            }

        }
        else
        {
            for (INetworkStatusChange network : objectList)
            {
                network.onNetworkStateChange(false, type);
            }
        }

    }
}

package com.familycircle.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.familycircle.TeamApp;

public class NetworkUtils {
	public static boolean IsNetworkAvailable(Context context)
	{

		ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null)
		{
			NetworkInfo nw = connectivityManager.getActiveNetworkInfo();
			if ((null != nw) && (nw.isConnected()))
			{
				return true;
			}
			return false;
		}
		return false;

	}
	
	public static boolean isWifiConnected()
	{
		ConnectivityManager connManager = (ConnectivityManager) TeamApp.getContext().getSystemService(TeamApp.getContext().CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnected();
	}

	public static boolean isMobileNetworkAvailable(Context context)
	{
		Log.d("NetworkUtils", "isMobileNetworkAvailable");
		if (context == null)
		{
			Log.d("NetworkUtils", "isMobileNetworkAvailable context if false");
			return false;
		}

		// if(IsNetworkAvailable(context) && isWiFiAvailable(context) == false){
		// return true;
		// }

		ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo nw = connectivityManager.getActiveNetworkInfo();
		if (nw != null)
		{
			int netType = nw.getType();
			if ((netType == ConnectivityManager.TYPE_MOBILE)
						|| (netType == ConnectivityManager.TYPE_MOBILE_DUN)
						|| (netType == ConnectivityManager.TYPE_MOBILE_HIPRI)
						|| (netType == ConnectivityManager.TYPE_MOBILE_MMS)
						|| (netType == ConnectivityManager.TYPE_MOBILE_SUPL))
			{
				Log.d("NetworkUtils","isMobileNetworkAvailable ConnectivityManager : netType = " + netType);
				return true;
			} else
			{

			}
		}
		boolean ret = false;
		TelephonyManager teleMan = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
		if (teleMan != null)
		{
			int networkType = teleMan.getNetworkType();
			if (networkType == TelephonyManager.NETWORK_TYPE_CDMA)
			{
				String id = teleMan.getDeviceId();
				if (id != null && Integer.valueOf(id) > 0)
				{
					return true;
				}
			}
			if (teleMan.getSimState() == TelephonyManager.SIM_STATE_READY)
			{
				// teleMan.
				switch (networkType)
				{
					case 7:
						// textV1.setText("1xRTT");
						ret = true;
						break;
					case 4:
						// textV1.setText("CDMA");
						ret = true;
						break;
					case 2:
						// textV1.setText("EDGE");
						ret = true;
						break;
					case 14:
						// textV1.setText("eHRPD");
						ret = true;
						break;
					case 5:
						// textV1.setText("EVDO rev. 0");
						ret = true;
						break;
					case 6:
						// textV1.setText("EVDO rev. A");
						ret = true;
						break;
					case 12:
						// textV1.setText("EVDO rev. B");
						ret = true;
						break;
					case 1:
						// textV1.setText("GPRS");
						ret = true;
						break;
					case 8:
						// textV1.setText("HSDPA");
						ret = true;
						break;
					case 10:
						ret = true;
						break;

					case 15:
						ret = true;
						break;
					case 9:
						ret = true;
						break;
					case 11:
						ret = true;
						break;
					case 13:
						ret = true;
						break;
					case 3:
						ret = true;
						break;
					case 0:
						ret = true;
						break;
				}
			}
		} else
		{
			Log.d("NetworkUtils","isMobileNetworkAvailable teleMan == null");
		}
		Log.d("NetworkUtils","isMobileNetworkAvailable ret = " + ret);
		return ret;
	}
}

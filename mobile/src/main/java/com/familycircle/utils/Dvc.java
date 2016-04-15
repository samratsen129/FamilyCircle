package com.familycircle.utils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.familycircle.lib.utils.Logger;
import com.familycircle.lib.utils.PrefManagerBase;

public class Dvc {
	private static String deviceUID = null;
	/**
	 * Returns the app version.
	 * @param ctx the caller's context.
	 * @return the appl version.
	 */
	public static String getAppVersion(Context ctx) { 
		
		String mAppVersion = null;
		
		try {
			mAppVersion = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), PackageManager.GET_META_DATA).versionName;
		} catch (Exception e) {
			Logger.e("Error in getting application version.", e);
		}
		
		if (mAppVersion == null) {
			mAppVersion = "";
		}
		
		return mAppVersion;
	}
	
	public static String getDeviceSerialId(){
		String serialId = null;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;

		if (currentapiVersion <= 8) {
			try {
				Class<?> c = Class.forName("android.os.SystemProperties");
				Method get = c.getMethod("get", String.class, String.class );
				serialId = (String)(   get.invoke(c, "ro.serialno", "unknown" )  );
			}
			catch (Exception ignored)
			{     
				serialId = null;
			}
		} else {
			serialId = android.os.Build.SERIAL;
		}
		
		if (serialId!=null)
			if (serialId.equalsIgnoreCase("unknown")) serialId = null;
		
		return serialId;
	}
	
	public static synchronized String getDeviceUid(Context ctx) {
		PrefManagerBase pm = new PrefManagerBase();
		
		if (deviceUID != null && !deviceUID.trim().isEmpty()) {
			try {
				Logger.d(" device id 00" + deviceUID);
				return deviceUID;
			} catch (Exception e) {
				Logger.d("Regenerating device id");
			}

		} else {
			String id = pm.getDeviceId();
			if (id!=null && !id.trim().isEmpty()){
				Logger.d(" device id 0" + id);
				return id;
			}
		}

		final TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		
		if (telephonyManager != null) {
			deviceUID = telephonyManager.getDeviceId();
		}
		
		if (deviceUID == null) { // check 1 of 2
			Logger.d(" device id 1");
			deviceUID = Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
			if (deviceUID == null) { // check 2 of 2
				Logger.d(" device id 2");
				deviceUID = String.valueOf(UUID.randomUUID());
			}
		}
		//String timeStr = "_"+(new Date()).getTime();
		pm.setDeviceId(deviceUID);
		Logger.d(" device id 2" + deviceUID);
		return deviceUID;
	}

}

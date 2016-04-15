package com.familycircle.lib.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.familycircle.sdk.BaseApp;


public class PrefManagerBase {
    private static final String TAG = "PrefManagerBase";
    protected final SharedPreferences mSettings;
	// aes key encryption key part
	private static final String D_USER_PREF= "u_pref";
    private static final String D_USER_PREF_LOGOFF= "u_pref_logoff";
    private static final String PREF_ID = "pref_id";
    private static final String PREF_SMS_LAST_QUERY_TIME = "SMS_LAST_QUERY_TIME";
    private static final String LOCAL_REMOTE_VIEW_SETTING= "LOCAL_REMOTE_SETTING";
    private static final String NOTIFICATION_AND_SOUND_PLAY_RINGTONE = "PLAY_RINGTONE";
    private static final String NOTIFICATION_AND_SOUND_RINGTONE_TYPE = "RINGTONE_TYPE";
    private static final String NOTIFICATION_AND_SOUND_VIBRATE_WHEN_RINGING = "VIBRATE_WHEN_RINGING";
    private static final String D_DVC_ID = "DEVICE_ID_PREFS";

	public PrefManagerBase() {
		mSettings = PreferenceManager.getDefaultSharedPreferences(BaseApp.getContext());

	}
	
	public void setUser(String s){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putString(D_USER_PREF, s);
            editor.commit();

        } catch (Exception e) {
            Log.d(TAG, "Unable to set id setting", e);
        }
	}
	
	public String getUser(){
        String v = mSettings.getString(D_USER_PREF, null);

        if (v!=null){
            try {
                return v;

            } catch (Exception e){
                Log.d(TAG, "Unable to resolve id setting", e);
            }

        }
        return null;
	}

    public void setUserLoggedOff(boolean s){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putBoolean(D_USER_PREF_LOGOFF, s);
            editor.commit();

        } catch (Exception e) {
            Log.d(TAG, "Unable to set logged off setting", e);
        }
    }

    public boolean getUserLoggedOff(){
        boolean v = mSettings.getBoolean(D_USER_PREF_LOGOFF, true);
        return v;
    }

    public synchronized int getNextId(){
        int v = mSettings.getInt(PREF_ID, 0);
        v++;
        setId(v);

        if (v>0){
            try {
                return v;

            } catch (Exception e){
                Log.d(TAG, "Unable to resolve id setting", e);
            }

        }
        return 0;
    }

    private void setId(int setting){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putInt(PREF_ID, setting);
            editor.commit();

        } catch (Exception e) {
            Log.d(TAG, "Unable to set id setting", e);
        }
    }

    public synchronized int getCurrentId(){
        int v = mSettings.getInt(PREF_ID, 0);

        if (v>0){
            try {
                return v;

            } catch (Exception e){
                Log.d(TAG, "Unable to resolve id setting", e);
            }

        }
        return 0;
    }

    public void setRingtoneNameSetting(String ringtone){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putString(NOTIFICATION_AND_SOUND_RINGTONE_TYPE, ringtone);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Unable to set ringtone type setting", e);
        }
    }

    public String getRingtoneNameSetting(){
        String result = null;

        try {
            result = mSettings.getString(NOTIFICATION_AND_SOUND_RINGTONE_TYPE, null);
        } catch (Exception e) {
            Log.e(TAG, "Unable to read ringtone type setting", e);
        }

        return result;
    }

    public void setVibrateWhenRingingSetting(boolean setting){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putBoolean(NOTIFICATION_AND_SOUND_VIBRATE_WHEN_RINGING, setting);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Unable to set vibrate when ringing setting", e);
        }
    }

    public boolean getVibrateWhenRingingSetting(){
        boolean result = false;

        try {
            result = mSettings.getBoolean(NOTIFICATION_AND_SOUND_VIBRATE_WHEN_RINGING, false);
        } catch (Exception e) {
            Log.e(TAG, "Unable to read vibrate when ringing setting", e);
        }

        return result;
    }


    public void setIsPlayRingtoneSetting(boolean setting){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putBoolean(NOTIFICATION_AND_SOUND_PLAY_RINGTONE, setting);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Unable to set play ringtone setting", e);
        }
    }

    public boolean getIsPlayRingtoneSetting(){
        boolean result = true;

        try {
            result = mSettings.getBoolean(NOTIFICATION_AND_SOUND_PLAY_RINGTONE, true);
        } catch (Exception e) {
            Log.e(TAG, "Unable to set play ringtone setting", e);
        }

        return result;
    }

    public void setIsLocalView(boolean setting){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putBoolean(LOCAL_REMOTE_VIEW_SETTING, setting);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Unable to set local/remote setting", e);
        }
    }

    public boolean getIsLocalView(){
        boolean result = true;

        try {
            result = mSettings.getBoolean(LOCAL_REMOTE_VIEW_SETTING, true);
        } catch (Exception e) {
            Log.e(TAG, "Unable to set local/remoe setting", e);
        }

        return result;
    }

    public void setLastQueryTime(long t){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putLong(PREF_SMS_LAST_QUERY_TIME, t);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Unable to set last query time", e);
        }
    }

    public long getLastQueryTime(long t){
        try {
            long result = mSettings.getLong(PREF_SMS_LAST_QUERY_TIME, 0);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Unable to set local/remoe setting", e);
        }
        return 0;
    }

    public void setDeviceId(String setting){
        SharedPreferences.Editor editor = mSettings.edit();

        try {
            editor.putString(D_DVC_ID, setting);
            editor.commit();

        } catch (Exception e) {
            Logger.d("Unable to set device id setting", e);
        }
    }

    public String getDeviceId(){
        String v = mSettings.getString(D_DVC_ID, null);

        if (v!=null){
            try {
                return v;

            } catch (Exception e){
                Logger.d("Unable to resolve device id setting", e);
            }

        }
        return null;
    }
}

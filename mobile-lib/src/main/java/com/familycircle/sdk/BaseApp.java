package com.familycircle.sdk;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.familycircle.lib.utils.Foreground;
import com.familycircle.lib.utils.db.DBHelper;
import com.familycircle.lib.utils.pubnub.PnRTCManager;

/**
 * Created by samratsen on 5/27/15.
 */
public class BaseApp extends MultiDexApplication {

    private static Context context;
    public static boolean startAudioRecording=false;

    public static String audioOutputFile = "/sdcard/DCIM/ouput_sound.mp4";
    public static double videoBytes;
    public static double audioBytes;
    private static String urlString;
    private static String companyGroupId = "mportal.com";
    protected static String mTag = "TeamApp"; // for the LogCat logging tag
    protected static int DEBUG_MODE = Log.DEBUG;
    protected static boolean LOG_CAPTURE = (DEBUG_MODE==Log.DEBUG);

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Foreground.init(this);
        PnRTCManager.getInstance(); // Bootstrapping
        DBHelper dbHelper = DBHelper.getInstance();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db!=null){
            db.close();
        }

    }

    public final static Context getContext()
    {
        return context;
    }

    public static String getUrl(){
        if (urlString==null||urlString.isEmpty()) {
            return "https://teampresence.mpsvcs.com/";
        } else {
            return urlString;
        }
    }

    protected static void setUrl(final String urlStr){

        urlString = urlStr;
    }

    protected static void setGroupId(final String groupId){
        companyGroupId = groupId;
    }

    public static String getGroupId(){
        return companyGroupId;
    }

    public static boolean isLOG_CAPTURE(){
        return LOG_CAPTURE;
    }

    public static String getLoggerTag(){
        return mTag;
    }

    public static int getDebugMode(){
        return DEBUG_MODE;
    }
}

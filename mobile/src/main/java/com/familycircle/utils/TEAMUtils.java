package com.familycircle.utils;


import android.util.Log;

import com.familycircle.TeamApp;
import com.familycircle.lib.utils.db.SmsDbAdapter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import me.leolin.shortcutbadger.ShortcutBadgeException;
import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Created by samratsen on 6/8/15.
 */
public class TEAMUtils {
    public static int updateSmsAppBadge(){
        int badgeCount = 0;
        try {
            SmsDbAdapter dbAdapter = new SmsDbAdapter();
            badgeCount = dbAdapter.getUnreadCount();
            ShortcutBadger.setBadge(TeamApp.getContext(), badgeCount);

        } catch (ShortcutBadgeException e) {
            Log.d("MainActivity", e.toString(), e);
        }
        return badgeCount;
    }

    public static void updateDialerAppBadge(int badgeCount){

        try {
            ShortcutBadger.setBadge(TeamApp.getContext(), badgeCount);

        } catch (ShortcutBadgeException e) {
            Log.d("SCUtils", e.toString(), e);
        }
    }

    public static String getFormattedLongTime(long timeValue){
        return String.format("%-13s", timeValue).replaceAll(" ", "0");
    }

    public static String getDateDescription(long datetime){
        // input date
        Calendar thenCal = new GregorianCalendar();
        thenCal.setTimeInMillis(datetime);
        Date thenDate = thenCal.getTime();

        // current date
        Calendar nowCal = new GregorianCalendar();
        nowCal.setTimeInMillis((new Date()).getTime());

        // yesterdays date
        Calendar prevCal = new GregorianCalendar();
        prevCal.add(Calendar.DATE, -1);

        if (thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                && thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
                && thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)) {
            return "Today";
        } else if (thenCal.get(Calendar.YEAR) == prevCal.get(Calendar.YEAR)
                && thenCal.get(Calendar.MONTH) == prevCal.get(Calendar.MONTH)
                && thenCal.get(Calendar.DAY_OF_MONTH) == prevCal.get(Calendar.DAY_OF_MONTH)) {
            return "Yesterday";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
            return dateFormat.format(thenDate);
        }

    }

    public static String capitalize(final String line) {
        if (line==null||line.trim().length()==0) return line;
        int size = line.length();
        return line.substring(0,1).toUpperCase() + (size>1?line.substring(1):"");
    }
}

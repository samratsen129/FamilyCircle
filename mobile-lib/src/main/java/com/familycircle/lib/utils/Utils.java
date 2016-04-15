package com.familycircle.lib.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.PhoneNumberUtils;
import android.view.Display;

import com.familycircle.sdk.BaseApp;
import com.familycircle.sdk.models.MessageModel;

/**
 * Created by samratsen on 2/27/15.
 */
public final class Utils {
    public static boolean isTabletDevice(Context context)
    {

        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /*
 * Formatting for displaying
 */
    public static String getFormattedPhoneNumber(String phoneNumberFormatted){
        try {
            return PhoneNumberUtils.formatNumber(phoneNumberFormatted.replaceFirst("^0+(?!$)", ""));

        } catch (Exception e){
            Logger.e("getFormattedPhoneNumber " + e.toString(), e);
        }

        return phoneNumberFormatted;
    }

    /**
     * @param phoneNumberFormatted
     * @return
     */
    public static String getPhoneNumberDigits(String phoneNumberFormatted){
        try {
            // removing all characters possible , based on inputType=phone in Android
            String _phoneNumberFormatted = phoneNumberFormatted.replaceAll("[-()+,;[A-Z].\\/\\n\\r\\t\\s\\#*]", "");
            _phoneNumberFormatted = _phoneNumberFormatted.replaceFirst("^0+(?!$)", "");//removing front zeroes
            // check for 1 in front, if there is no 1 then have to add a 1
            if (!_phoneNumberFormatted.startsWith("1")&& !_phoneNumberFormatted.startsWith("0")){
                _phoneNumberFormatted = "1"+_phoneNumberFormatted;
            }
            long l = Long.parseLong(_phoneNumberFormatted);
            _phoneNumberFormatted = String.format("%013d", l);
            return _phoneNumberFormatted;
        } catch (Exception e){

        }

        return phoneNumberFormatted;
    }

    public static int generateMid(MessageModel message) {
        return (int)(message.getTime() % 10000L);
    }

}

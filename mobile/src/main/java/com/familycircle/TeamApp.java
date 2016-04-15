package com.familycircle;

import com.familycircle.lib.utils.pubnub.PnRTCManager;
import com.familycircle.manager.TeamManager;
import com.familycircle.lib.utils.Foreground;
import com.familycircle.sdk.BaseApp;
import com.familycircle.utils.DBInterface;
import com.parse.Parse;

/**
 * Created by samratsen on 5/28/15.
 */
public class TeamApp extends BaseApp {

    // For tracking time since last notification sound, so that notification sound does not keep on dinging
    public static long lastNotificationSound = 0;

    @Override
    public void onCreate(){
        super.onCreate();
        TeamManager.getInstance();
        Foreground.init(this);
        DBInterface.parseInit(getContext());
        PnRTCManager.getInstance();
    }
}

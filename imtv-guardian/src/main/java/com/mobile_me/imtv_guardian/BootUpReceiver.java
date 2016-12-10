package com.mobile_me.imtv_guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by pasha on 9/19/16.
 */
public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //BootUpReceiver.setAlarmToStart(context);
        Intent i = new Intent(context, GuardianLogoActivity.class);  //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.startActivity(i);
    }

}

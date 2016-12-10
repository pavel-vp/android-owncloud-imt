package com.mobile_me.imtv_player.ui;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.mobile_me.imtv_player.util.CustomExceptionHandler;

/**
 * Created by pasha on 8/17/16.
 */
public class IMTVApplication extends Application {
    private static IMTVApplication instance;

    protected Handler handler = new Handler();

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());
        super.onCreate();
    }

    public static IMTVApplication getInstance() {
        return instance;
    }

    public IMTVApplication() {
        super();
        instance = this;
    }

    /*
    public static void reLaunch(Context context) {
        CustomExceptionHandler.log("reLaunch");
        Context ctx = context == null ? IMTVApplication.getInstance() : context;
        Intent i = new Intent(ctx, LogoActivity.class);  //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        ctx.startActivity(i);
    }

    public static void setAlarmToStart(Context context) {
        Context ctx = context == null ? IMTVApplication.getInstance() : context;
        CustomExceptionHandler.log("set alarm to start");
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, LogoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT );
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000,  pi);
        CustomExceptionHandler.log("set alarm to start done");

    }
*/

}

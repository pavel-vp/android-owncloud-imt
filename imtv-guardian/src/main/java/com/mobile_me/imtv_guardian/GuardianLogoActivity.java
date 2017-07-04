package com.mobile_me.imtv_guardian;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GuardianLogoActivity extends Activity {

    TextView tvDetail;
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_guardian_logo);
        tvDetail = (TextView)findViewById(R.id.tvDetail);

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cycleWatch();
            }
        }).start();
    }

    private boolean isPlayerRunning() {
        boolean res = false;
        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++)
        {
            if(procInfos.get(i).processName.equals("com.mobile_me.imtv_player")
                    && procInfos.get(i).importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
            {
                res = true;
            }
        }
        return res;
    }

    private void checkAndRun() {
        if (!isPlayerRunning()) {
            Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.mobile_me.imtv_player");
            if (LaunchIntent != null) {
                LaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                startActivity(LaunchIntent);
                Date now  = Calendar.getInstance().getTime();
                String s = sdf.format(now);
                tvDetail.setText("guardian ("+ s + ")");
            }
        }
    }

    private void cycleWatch() {
        while(true) {
            try {
                checkAndRun();
                Thread.yield();
                Thread.sleep(60000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

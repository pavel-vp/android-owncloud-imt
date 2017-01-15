package com.mobile_me.imtv_player.dao;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.os.EnvironmentCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.mobile_me.imtv_player.BuildConfig;
import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.model.MTGlobalSetupRec;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.service.LogUpload;
import com.mobile_me.imtv_player.service.MTPlayListManager;
import com.mobile_me.imtv_player.service.SettingsLoader;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.mobile_me.imtv_player.util.RootUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Отвечает за локальные данные
 * Created by pasha on 8/14/16.
 */
public class Dao {

    public final static String DB_NAME = "imtv_db";
    public final static int DB_VERSION = 1;
    public final static String PATH_KEY = "path_key";
    public final static String DEVICEID_KEY = "deviceid_key";
    public final static String LASTTIMESETTINGS_KEY = "lasttimesettings_key";
    public final static String MIN_COUNT_FREE = "min_count_free";
    public final static String COUNT_DAYS_BEFORE = "count_days_before";

    private static Dao instance;

    private Context ctx;

    private PlayListDBHelper mPlayListDBHelper;
    private StatisticDBHelper mStatisticDBHelper;
    private SharedPreferences mSharedPreferences;
    File downFolder;
    String remotePlayListFilePath;
    String remotePlayList2FilePath;
    String remoteUpdateFilePath;
    String remoteVideoDir;
    String remoteSettingsInFilePath;
    String remoteSettingsNewFilePath;
    String downVideoFolder;
    String deviceId;
    private MTPlayListManager playListManager;
    private MTPlayListManager playListManager2;
    private Boolean isTerminated = false;
    private Long lastTimeSettings = null;
    private MTGlobalSetupRec setupRec;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static Dao getInstance(Context ctx) {
        if (instance == null) {
            instance = new Dao(ctx);
        }
        return instance;
    }

    public Dao(Context ctx) {
        this.ctx = ctx;
        this.mPlayListDBHelper = new PlayListDBHelper(this.ctx);
        this.mStatisticDBHelper = new StatisticDBHelper(this.ctx);
        this.mSharedPreferences = ctx.getSharedPreferences("settings", Activity.MODE_PRIVATE);
        this.setupRec = new MTGlobalSetupRec();
        try {
            setupRec.setMin_count_free(this.mSharedPreferences.getLong(MIN_COUNT_FREE, -1));
            setupRec.setCount_days_before(this.mSharedPreferences.getLong(COUNT_DAYS_BEFORE, -1));
        } catch (Exception e) {
            CustomExceptionHandler.log("no previous setup rec");
        }

/*        TelephonyManager telephonyManager = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = telephonyManager.getDeviceId();
        */
        WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        deviceId = info.getMacAddress().replace(":","");
//deviceId = "352005048247251";
        CustomExceptionHandler.log("version:"+BuildConfig.VERSION_CODE);
        CustomExceptionHandler.log("deviceId:"+deviceId);
        String savedDeviceID = mSharedPreferences.getString(DEVICEID_KEY, null);
        CustomExceptionHandler.log("savedDeviceID:"+savedDeviceID);
        if (savedDeviceID != null) {
            if (deviceId == null) {
                deviceId = savedDeviceID;
            }
            if (!deviceId.equals(savedDeviceID)) {
                Toast.makeText(ctx, "Необходимо переустановить приложение!", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
        } else {
            SharedPreferences.Editor ed = mSharedPreferences.edit();
            ed.putString(DEVICEID_KEY, deviceId);
            ed.commit();
        }

        String lts = mSharedPreferences.getString(LASTTIMESETTINGS_KEY, null);
        if (lts != null) {
            lastTimeSettings = Long.parseLong(lts);
            // Если интервал превышает максимальный - обнулить
            Date ltsD = new Date(lastTimeSettings + Integer.parseInt(ctx.getString(R.string.globalini_expire_interval_minutes)) * 60 * 1000);
            if (ltsD.before(Calendar.getInstance().getTime())) {
                setLastTimeSettings(null);
            }
        }

        CustomExceptionHandler.log("lastTimeSettings:"+lastTimeSettings);
        CustomExceptionHandler.log("is device rooted="+ RootUtils.isDeviceRooted());
//        downFolder = act.getExternalFilesDir(act.getString(R.string.download_folder_path));
        downFolder = definePathToVideo();
        downFolder.mkdir();
        remotePlayListFilePath = "/" + String.format(ctx.getString(R.string.playlist_filepath), deviceId);
        remotePlayList2FilePath = "/" + String.format(ctx.getString(R.string.playlist2_filepath), deviceId);
        remoteUpdateFilePath = "/" + ctx.getString(R.string.updateapk_filepath);
        remoteVideoDir = "/" + ctx.getString(R.string.video_dir) ;
        remoteSettingsInFilePath = "/" + String.format(ctx.getString(R.string.settings_in_filepath), deviceId);
        remoteSettingsNewFilePath = "/" + String.format(ctx.getString(R.string.settings_new_filepath), deviceId);

        // создадим директории
        File dv = new File(downFolder, ctx.getString(R.string.video_dir));
        dv.mkdir();
        downVideoFolder = dv.getAbsolutePath();
        File dp = new File(new File(downFolder, String.format(ctx.getString(R.string.playlist_filepath), deviceId)).getParent());
        dp.mkdir();
        CustomExceptionHandler.log("DAO created");
        CustomExceptionHandler.log("downFolder="+downFolder);
        CustomExceptionHandler.log("remotePlayListFilePath="+remotePlayListFilePath);
        CustomExceptionHandler.log("remotePlayList2FilePath="+remotePlayList2FilePath);
        CustomExceptionHandler.log("remoteUpdateFilePath="+remoteUpdateFilePath);
        CustomExceptionHandler.log("remoteVideoDir="+remoteVideoDir);
        CustomExceptionHandler.log("downVideoFolder="+downVideoFolder);
        CustomExceptionHandler.log("remoteSettingsInFilePath="+remoteSettingsInFilePath);
        CustomExceptionHandler.log("remoteSettingsNewFilePath="+remoteSettingsNewFilePath);

    }




    public File definePathToVideo() {
        File path = null;
        String p = mSharedPreferences.getString(PATH_KEY, null);
        if (p != null) {
            path = new File(p);
        }
        CustomExceptionHandler.log("definePathToVideo stored path="+path);
        // если пусто
        if (p == null || !Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(path))
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(EnvironmentCompat.getStorageState(path))
                || !path.exists()
                || !path.canWrite() ) {
            path = getBestAvailableFilesRoot();
            CustomExceptionHandler.log("definePathToVideo getbestpath path="+path);
            SharedPreferences.Editor ed = mSharedPreferences.edit();
            ed.putString(PATH_KEY, path.getAbsolutePath());
            ed.commit();
        }
        path.mkdirs();
        return path;
    }

    public File getBestAvailableFilesRoot() {
        // FIXME: переделать на приоритеты (usb - 1, ext_sd - 2, emulated - 3, если нет ничего - то локальное)

        File[] roots = new File[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            roots = ctx.getExternalFilesDirs(null);
        } else {
            roots = ContextCompat.getExternalFilesDirs(ctx, null);
        }
        if (roots != null) {
            File best = null;
            for (File root : roots) {
                if (root == null) {
                    continue;
                }

                if (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(root))
                        &&  !Environment.MEDIA_MOUNTED_READ_ONLY.equals(EnvironmentCompat.getStorageState(root))
                        && root.exists()
                        && root.canWrite() ) {
                    CustomExceptionHandler.log("getBestAvailableFilesRoot root="+root);
                    long freeSize = root.getFreeSpace();
                    File dv = new File(root, ctx.getString(R.string.video_dir));
                    boolean res = dv.mkdirs();
                    if (res && (best == null  || freeSize > best.getFreeSpace())) {
                        dv.delete();
                        best = root;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        // Worst case, resort to internal storage
        return ctx.getFilesDir();
    }

    public PlayListDBHelper getPlayListDBHelper() {
        return mPlayListDBHelper;
    }

    public String getDownVideoFolder() {
        return downVideoFolder;
    }


    public String getRemotePlayListFilePath() {
        return remotePlayListFilePath;
    }


    public File getDownFolder() {
        return downFolder;
    }


    public String getRemoteVideoDir() {
        return remoteVideoDir;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRemotePlayList2FilePath() {
        return remotePlayList2FilePath;
    }


    public MTPlayListManager getPlayListManager() {
        if (playListManager == null) {
            playListManager = new MTPlayListManager(this.ctx, MTPlayList.TYPEPLAYLIST_1);
        }
        return playListManager;
    }

    public MTPlayListManager getPlayListManager2() {
        if (playListManager2 == null) {
            playListManager2 = new MTPlayListManager(this.ctx, MTPlayList.TYPEPLAYLIST_2);
        }
        return playListManager2;
    }

    public MTPlayListManager getPlayListManagerByType(int playListType) {
        if (playListType == MTPlayList.TYPEPLAYLIST_2) {
            return getPlayListManager2();
        }
        return getPlayListManager();
    }

    public String getRemoteUpdateFilePath() {
        return remoteUpdateFilePath;
    }


    public Context getContext() {
        return ctx;
    }


    public String getRemoteSettingsInFilePath() {
        return remoteSettingsInFilePath;
    }

    public String getRemoteSettingsNewFilePath() {
        return remoteSettingsNewFilePath;
    }

    public Boolean getTerminated() {
        return isTerminated;
    }

    public void setTerminated(Boolean terminated) {
        isTerminated = terminated;
    }


    public Long getLastTimeSettings() {
        return lastTimeSettings;
    }

    public void setLastTimeSettings(Long lastTimeSettings) {
        this.lastTimeSettings = lastTimeSettings;
        CustomExceptionHandler.log("set savedDeviceID to :"+lastTimeSettings);
        SharedPreferences.Editor ed = mSharedPreferences.edit();
        if (lastTimeSettings == null) {
            ed.putString(LASTTIMESETTINGS_KEY, null);
        } else {
            ed.putString(LASTTIMESETTINGS_KEY, Long.toString(lastTimeSettings));
        }
        ed.commit();
    }

    public ScheduledExecutorService getExecutor() {
        return this.executorService;
    }


    public StatisticDBHelper getmStatisticDBHelper() {
        return mStatisticDBHelper;
    }

    public MTGlobalSetupRec getSetupRec() {
        return setupRec;
    }

    public void setSetupRec(MTGlobalSetupRec setupRec) {
        this.setupRec = setupRec;
        SharedPreferences.Editor ed = mSharedPreferences.edit();
        ed.putString(MIN_COUNT_FREE, setupRec == null ? null : Long.toString(setupRec.getMin_count_free()));
        ed.putString(COUNT_DAYS_BEFORE, setupRec == null ? null : Long.toString(setupRec.getCount_days_before()));
        ed.commit();
    }

}

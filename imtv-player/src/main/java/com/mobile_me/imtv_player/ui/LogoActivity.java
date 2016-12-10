package com.mobile_me.imtv_player.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.service.IMTCallbackEvent;
import com.mobile_me.imtv_player.service.LogUpload;
import com.mobile_me.imtv_player.service.MTOwnCloudHelper;
import com.mobile_me.imtv_player.service.NewRegisterUpload;
import com.mobile_me.imtv_player.service.SettingsLoader;
import com.mobile_me.imtv_player.service.Updater;
import com.mobile_me.imtv_player.service.tasks.CheckPlayListLocalTask;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by pasha on 8/13/16.
 */
public class LogoActivity extends AbstractBaseActivity implements IMTCallbackEvent {

    private static final int MODE_CHECK_SETTINGS = 0;
    private static final int MODE_REQUEST_NEW = 1;
    private static final int MODE_WAIT_FOR_SETTINGS = 2;
    private static final int MODE_SETTINGS_OK = 3;

    private static final int ACTION_SETINGS_LOCAL_OK = 1;
    private static final int ACTION_SETINGS_LOCAL_FAIL = 2;
    private static final int ACTION_UPLOAD_NEW = 3;
    private static final int ACTION_UPLOAD_OK = 4;
    private static final int ACTION_UPLOAD_FAIL = 5;
    private static final int ACTION_DOWNLOAD_SETTINGS = 6;
    private static final int ACTION_DOWNLOAD_SETTINGS_OK = 7;
    private static final int ACTION_DOWNLOAD_SETTINGS_FAIL = 8;

    List<MTOwnCloudHelper> helpers = new ArrayList<>();
    List<Boolean> loadedCompleted = new ArrayList<>();
    private Boolean isActive = false;
    private int mode = MODE_CHECK_SETTINGS;


    private TextView tvVer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());
        setContentView(R.layout.activity_logo);
        tvVer = (TextView)findViewById(R.id.tvVer);
        try {
            PackageInfo currentInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            tvVer.setText("ver:" + currentInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        helpers.add(new MTOwnCloudHelper(Dao.getInstance(this).getRemotePlayListFilePath(), this, this));
        helpers.add(new MTOwnCloudHelper(Dao.getInstance(this).getRemotePlayList2FilePath(), this, this));
        loadedCompleted.add(false);
        loadedCompleted.add(false);
        CustomExceptionHandler.log("logo created");

        CustomExceptionHandler.log("start updater");
        Updater updater = new Updater(this);
        updater.startDownLoadUpdate();

        CustomExceptionHandler.log("start logupdater");
        LogUpload.getInstance(Dao.getInstance(this)).reLaunchUploadLog();

        CustomExceptionHandler.log("start settings loader");
        SettingsLoader.getInstace(Dao.getInstance(this)).tryLoadSettings();

        if (Dao.getInstance(this).getLastTimeSettings() == null) {
            CustomExceptionHandler.log("start NewRegisterUpload because it need to");
            NewRegisterUpload.getInstance(Dao.getInstance(this)).startRegisterAndUpload();
        }
    }

    private void handleModeByAction(int action) {
        switch (action) {
            case ACTION_SETINGS_LOCAL_OK:
                // настройки загружены ранее


        }

    }

    private boolean isAllCompleted() {
        boolean res = true;
        for (Boolean r : loadedCompleted) {
            res = res && r;
        }
        return res;
    }

    private int getPLayListTypeByOwnHandler(MTOwnCloudHelper helper) {
        int idx = 1;
        for (MTOwnCloudHelper h : helpers) {
            if (h == helper) {
                return idx;
            }
            idx++;
        }
        return 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
        // стартовать task по анализу доступности данных для проигрывания локально, чтобы не ждать загрузки
        runTaskInBackgroundNoDialog(new CheckPlayListLocalTask(MTPlayList.TYPEPLAYLIST_1));
        CustomExceptionHandler.log("task 1 started");
        Toast.makeText(this, Dao.getInstance(this).getDeviceId(), Toast.LENGTH_LONG).show();
    }

    private void tryStartMainActivity() {
        if (Integer.parseInt(getResources().getString(R.string.playlists_count)) > 1) {
            // если должно быть более чем 1, а закачалось меньше - запустим второй
            if (!isAllCompleted()) {
                runTaskInBackgroundNoDialog(new CheckPlayListLocalTask(MTPlayList.TYPEPLAYLIST_2));
                return;
            }
        }
        // если нет
        // есть локальный плейлист - проверяем наличие файлов на диске
        Intent in = new Intent();
        in.setClass(this, MainActivity2.class);
        startActivity(in);
        this.finish(); // закрываем это активити
    }

    @Override
    protected void onBackgroundTaskComplete(int taskId, Runnable task) {
        super.onBackgroundTaskComplete(taskId, task);
        if (task instanceof CheckPlayListLocalTask) {
            boolean r = ((CheckPlayListLocalTask) task).result;
            int type = ((CheckPlayListLocalTask) task).playListType;
            CustomExceptionHandler.log("onBackgroundTaskComplete, r="+ r + ", task="+task);
            if (r) {
                Dao.getInstance(this).getPlayListManagerByType(type).mergeAndSavePlayList(((CheckPlayListLocalTask) task).playList);
                loadedCompleted.set(type-1, true);
                tryStartMainActivity();
            } else {
                // нет локального плейлиста - запускаем задачу первоначальной загрузки в текущем активити (пусть будет логотип)
                helpers.get(type - 1).loadPlayListFromServer();
            }
        }
    }

    @Override
    public void onPlayListLoaded(MTPlayList playListNew, MTOwnCloudHelper ownCloudHelper) {
        // плейлист загружен, запускаем загрузку первого файла
        int typePlayList = getPLayListTypeByOwnHandler(ownCloudHelper);
        CustomExceptionHandler.log("onPlayListLoaded success. playListNew.size="+playListNew.getPlaylist().size()+", typePlayList="+typePlayList);
        //Toast.makeText(this, "Плейлист загружен", Toast.LENGTH_SHORT).show();
        // запустить загрузку файлов из плейлиста
        playListNew.setTypePlayList(typePlayList);
        Dao.getInstance(this).getPlayListManagerByType(typePlayList).mergeAndSavePlayList(playListNew);
        MTPlayListRec fileToLoad = Dao.getInstance(this).getPlayListManagerByType(typePlayList).getNextFileToLoad();
        CustomExceptionHandler.log("fileToLoad="+fileToLoad);
        if (fileToLoad != null) {
            ownCloudHelper.loadVideoFileFromPlayList(fileToLoad);
        } else {
             // если все файлы актуальные вызываем активити
            loadedCompleted.set(typePlayList-1, true);
            tryStartMainActivity();
        }
    }

    @Override
    public void onVideoFileLoaded(MTPlayListRec file, MTOwnCloudHelper helper) {
        int typePlayList = getPLayListTypeByOwnHandler(helper);
        CustomExceptionHandler.log("typePlayList="+typePlayList);
        loadedCompleted.set(typePlayList-1, true);
        Dao.getInstance(this).getPlayListManagerByType(typePlayList).setFileStateFlag(file, MTPlayListRec.STATE_UPTODATE);
        tryStartMainActivity();
    }

    @Override
    public void onUpdateFileLoaded(MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onError(int mode, MTOwnCloudHelper helper, RemoteOperationResult result) {
        final int typePlayList = getPLayListTypeByOwnHandler(helper);
        // запустить заново загрузку
        Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                runTaskInBackgroundNoDialog(new CheckPlayListLocalTask(typePlayList));
            }
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onUploadLog(String localFileToUpload) {

    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }

}

package com.mobile_me.imtv_player.service;

import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by pasha on 03.11.16.
 */
public class NewRegisterUpload implements IMTCallbackEvent {

    Dao dao;
    MTOwnCloudHelper helper;
    SimpleDateFormat sdf;
    private boolean success = false;

    private static NewRegisterUpload instance;

    public static NewRegisterUpload getInstance(Dao dao) {
        if (instance == null) {
            instance = new NewRegisterUpload(dao);
        }
        return instance;
    }

    public NewRegisterUpload(Dao dao) {
        this.dao = dao;
        helper = new MTOwnCloudHelper(null, dao.getContext(), this);
        sdf = new SimpleDateFormat("yyMMdd-HHmmss");
    }

    public void startRegisterAndUpload() {
        try {
            // Процесс фоновой загрузки логов на сервер
            CustomExceptionHandler.log("try startRegisterAndUpload");

            File tmpFile = new File(dao.getDownFolder(), dao.getRemoteSettingsNewFilePath());
            tmpFile.createNewFile();

            CustomExceptionHandler.log("created empty file tmpFile=" + tmpFile.getAbsolutePath());
            // Отправить
            helper.uploadLogToServer(dao.getRemoteSettingsNewFilePath());
        } catch (Exception e) {
            CustomExceptionHandler.logException("ошибка при отправке файла", e);
        }
    }

    @Override
    public void onPlayListLoaded(MTPlayList playList, MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onVideoFileLoaded(MTPlayListRec file, MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onUpdateFileLoaded(MTOwnCloudHelper ownCloudHelper) {
        CustomExceptionHandler.log("newregisterUpload file uploaded");
        success = true;
    }

    @Override
    public void onError(int mode, MTOwnCloudHelper ownCloudHelper, RemoteOperationResult result) {
        CustomExceptionHandler.log("newregisterUpload file upload error, starting again");
        startRegisterAndUpload();
    }

    @Override
    public void onUploadLog(String uploadedLocalFile) {

    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }


    public boolean isSuccess() {
        return success;
    }

}

package com.mobile_me.imtv_player.service;

import android.os.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.dao.StatisticDBHelper;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by pasha on 04.03.17.
 */
public class StatUpload implements IMTCallbackEvent  {

    Dao dao;
    MTOwnCloudHelper helper;
    SimpleDateFormat sdf;
    private Long lastIDExported = null;
    private File lastFile = null;

    private static StatUpload instance;


    public static StatUpload getInstance(Dao dao) {
        if (instance == null) {
            instance = new StatUpload(dao);
        }
        return instance;
    }

    public StatUpload(Dao dao) {
        this.dao = dao;
        helper = new MTOwnCloudHelper(null, dao.getContext(), this);
        sdf = new SimpleDateFormat("yyMMdd-HHmmss");
    }

    public void startUploadStat() {
        try {
            // Процесс фоновой загрузки статистики проигрывания на сервер
            CustomExceptionHandler.log("try startUpload stat");
            lastIDExported = null;
            lastFile = null;
            String fileTempate = dao.getDeviceId() + "_" + sdf.format(Calendar.getInstance().getTime());

            String path = Environment.getExternalStorageDirectory() + dao.getContext().getString(R.string.stat_folder_path);
            File filePath = new File(path);

            String tmpZipFileName =  fileTempate + ".zip";

            File tmpFile = new File(filePath, tmpZipFileName);
            File onlyDir = new File(tmpFile.getParent());

            onlyDir.mkdirs();
            tmpFile.delete();

            CustomExceptionHandler.log("tmpZipFileName=" + tmpFile.getAbsolutePath());
            // Пожать файл с логом
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            ZipEntry e = new ZipEntry(fileTempate + ".log");
            zos.putNextEntry(e);
            // запишем данные статистики из лок.базы в файл
            List<StatisticDBHelper.MTStatRec> list = dao.getmStatisticDBHelper().getNotExportedStatList();
            String res = "";
            if (list.size() > 0) {

                StatisticDBHelper.MTStatRec[] arr = list.toArray(new StatisticDBHelper.MTStatRec[list.size()]);
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    res = mapper.writeValueAsString(arr);
                    lastIDExported = arr[arr.length-1].getIdx();
                } catch (Exception e1) {
                    CustomExceptionHandler.logException("encode array exception ", e1);
                }

            }

            zos.write(res.getBytes());

            zos.closeEntry();
            zos.flush();
            zos.close();


            CustomExceptionHandler.log("start write");
            CustomExceptionHandler.log("send file=" + tmpFile.getAbsolutePath());

            // отошлем файл
            helper.uploadLogToServer(tmpFile.getAbsolutePath(), dao.getContext().getResources().getString(R.string.uploadstat_dir));
            // чтобы удалить потом
            lastFile = tmpFile;

        } catch (Exception e) {
            CustomExceptionHandler.logException("ошибка при отправке статистики", e);
            reLaunchUploadStat();
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

    }

    @Override
    public void onError(int mode, MTOwnCloudHelper ownCloudHelper, RemoteOperationResult result) {
        CustomExceptionHandler.log("stat file upload error");
        reLaunchUploadStat();
    }

    @Override
    public void onUploadLog(String uploadedLocalFile) {
        // после успеха - сделать снова запуск через определенное число минут
        CustomExceptionHandler.log("stat file upload success");
        // проставим флаг экспорта
        if (lastIDExported != null) {
            dao.getmStatisticDBHelper().clearExportedStatList(lastIDExported);
        }
        if (lastFile != null) {
            lastFile.delete();
            CustomExceptionHandler.log("stat file deleted");
        }
        reLaunchUploadStat();
    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }

    @Override
    public void onFileInfoLoaded(RemoteFile fileInfo) {

    }

    public void reLaunchUploadStat() {
        if (!dao.getTerminated()) {
            CustomExceptionHandler.log("Relaunch upload stat");
            dao.getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    StatUpload.this.startUploadStat();
                }
            }, (dao.getSetupRec() == null || dao.getSetupRec().getStats_send_time() == null) ? 30 : dao.getSetupRec().getStats_send_time(), TimeUnit.MINUTES);
        }
    }

}

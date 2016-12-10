package com.mobile_me.imtv_player.service;

import com.mobile_me.imtv_player.R;
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
        import java.util.concurrent.Executors;
        import java.util.concurrent.TimeUnit;
        import java.util.zip.ZipEntry;
        import java.util.zip.ZipOutputStream;

public class LogUpload implements IMTCallbackEvent {
    Dao dao;
    MTOwnCloudHelper helper;
    SimpleDateFormat sdf;

    private static LogUpload instance;

    public static LogUpload getInstance(Dao dao) {
        if (instance == null) {
            instance = new LogUpload(dao);
        }
        return instance;
    }

    public LogUpload(Dao dao) {
        this.dao = dao;
        helper = new MTOwnCloudHelper(null, dao.getContext(), this);
        sdf = new SimpleDateFormat("yyMMdd-HHmmss");
    }

    public void startUpload() {
        try {
            // Процесс фоновой загрузки логов на сервер
            CustomExceptionHandler.log("try startUpload");

            String zipPostfix = dao.getDeviceId() + "_" + sdf.format(Calendar.getInstance().getTime());

            String logFileName = CustomExceptionHandler.getLog().getFileName();
            String tmpZipFileName = logFileName + "_" + zipPostfix + ".zip";
            File tmpFile = new File(tmpZipFileName);

            if (tmpFile.exists())
                tmpFile.delete();

            CustomExceptionHandler.log("tmpZipFileName=" + tmpZipFileName);
            // Пожать файл с логом
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            ZipEntry e = new ZipEntry(zipPostfix + ".log");
            zos.putNextEntry(e);

            CustomExceptionHandler.log("start write");
            CustomExceptionHandler.log("send file=" + tmpZipFileName);

            // переключить на временный файл
            String logFileNameTmp = CustomExceptionHandler.getLog().switchToNewFile();
            File logFile = new File(logFileNameTmp);


            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logFile));
            byte[] br = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(br)) != -1) {
                zos.write(br, 0, bytesRead);
            }

            bis.close();

            zos.flush();
            zos.close();
            // Удалить файл временный
            logFile.delete();

            // Отправить пожатый файл с логом
            // TODO: отправить все файлы zip в этой директории
            helper.uploadLogToServer(tmpZipFileName);
        } catch (Exception e) {
            CustomExceptionHandler.logException("ошибка при отправке лога", e);
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
        reLaunchUploadLog();
    }

    @Override
    public void onUploadLog(String localFileToUpload) {
        CustomExceptionHandler.log("sent = "+localFileToUpload);
        File tmpFile = new File(localFileToUpload);
        tmpFile.delete();
        // заново поставить задание на отсылку лога
        reLaunchUploadLog();

    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }

    public void reLaunchUploadLog() {
        if (!dao.getTerminated()) {
            CustomExceptionHandler.log("Relaunch upload log");
            dao.getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    LogUpload.this.startUpload();
                }
            }, Integer.parseInt(dao.getContext().getResources().getString(R.string.uploadlogs_interval_minutes)), TimeUnit.MINUTES);
        }
    }

}

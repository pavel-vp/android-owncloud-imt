package com.mobile_me.imtv_player.service;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.model.MTPointRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by pasha on 7/21/16.
 */
public class MTOwnCloudHelper implements OnRemoteOperationListener, OnDatatransferProgressListener  {
    public static final int TYPEFILE_SIMPLE = 1;
    public static final int TYPEFILE_PLAYLIST = 2;

    private static final String LOG_TAG = MTOwnCloudHelper.class.getCanonicalName();

    private static final int STATE_UNKNOWN = 0;
    private static final int STATE_LOADPLAYLIST = 1;
    private static final int STATE_LOADVIDEOFILE = 2;
    private static final int STATE_LOADUPDATE = 3;
    private static final int STATE_LOADSIMPLEFILE = 4;
    private static final int STATE_UPLOADLOG = 5;
    private static final int STATE_LOADFILEINFO = 6;


    Context ctx;
    private OwnCloudClient mClient;
    private IMTCallbackEvent cb;
    Handler mHandler = new Handler();
    ObjectMapper mapper = new ObjectMapper();

    private int state = STATE_UNKNOWN;
    Dao dao;
    MTPlayListRec fileToLoad;
    String playListRemotePath;
    String localFileToUpload;
    private int typeFile = TYPEFILE_PLAYLIST;

    public MTOwnCloudHelper(String playListRemotePath, Context ctx, IMTCallbackEvent cb) {
        this(playListRemotePath, ctx, cb, TYPEFILE_PLAYLIST);
    }

    public MTOwnCloudHelper(String playListRemotePath, Context ctx, IMTCallbackEvent cb, int typeFile) {
        this.ctx = ctx;
        this.cb = cb;
        this.playListRemotePath = playListRemotePath;
        this.typeFile = typeFile;
        dao = Dao.getInstance(ctx);
        Uri serverUri = Uri.parse(ctx.getString(R.string.server_base_url));
        mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, ctx, true);
        mClient.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(
                        ctx.getString(R.string.username),
                        ctx.getString(R.string.password)
                )
        );
        mClient.getParams().setSoTimeout(30 * 60 * 1000);
    }

    public void loadPlayListFromServer() {
        if (state == STATE_UNKNOWN) {
            state = STATE_LOADPLAYLIST;
            DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(playListRemotePath, dao.getDownFolder().getAbsolutePath());
            downloadOperation.addDatatransferProgressListener(this);
            downloadOperation.execute(mClient, this, mHandler);
            CustomExceptionHandler.log("loadPlayListFromServer started=" + playListRemotePath + " to " + dao.getDownFolder().getAbsolutePath());
        }
    }

    public void loadSimpleFileFromServer() {
        if (state == STATE_UNKNOWN) {
            state = STATE_LOADSIMPLEFILE;
            DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(playListRemotePath, dao.getDownFolder().getAbsolutePath());
            downloadOperation.addDatatransferProgressListener(this);
            downloadOperation.execute(mClient, this, mHandler);
            CustomExceptionHandler.log("loadPlayListFromServer started=" + playListRemotePath + " to " + dao.getDownFolder().getAbsolutePath());
        }
    }

    public void uploadLogToServer(String file, String remoteDir) {
        if (state == STATE_UNKNOWN) {
            state = STATE_UPLOADLOG;
            File f = new File(file);
            localFileToUpload = file;
            String remotePath = remoteDir + "/" + f.getName();
            UploadRemoteFileOperation uploadRemoteFileOperation = new UploadRemoteFileOperation(file, remotePath, "binary/octet-stream");
            uploadRemoteFileOperation.addDatatransferProgressListener(this);
            uploadRemoteFileOperation.execute(mClient, this, mHandler);
        }
    }

    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileAbsoluteName) {
//        cb.log("onTransferProgress="+fileAbsoluteName+ "  "+totalTransferredSoFar);
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (!result.isSuccess()) {
          //  Toast.makeText(ctx, R.string.todo_operation_finished_in_fail, Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, result.getLogMessage(), result.getException());
            CustomExceptionHandler.log("onRemoteOperationFinish error="+result.getLogMessage());
            state = STATE_UNKNOWN;
            cb.onError(state, this,  result);
        } else if (operation instanceof ReadRemoteFileOperation ) {
            CustomExceptionHandler.log("onReadRemoteOperationFinish success. state="+state);
            switch (state) {
                case STATE_LOADFILEINFO:
                    state = STATE_UNKNOWN;
                    // загрузили информацио о файле
                    // событие обработчик
                    cb.onFileInfoLoaded((RemoteFile) (result.getData().get(0)));
                    break;
            }

        } else if (operation instanceof DownloadRemoteFileOperation ) {
            CustomExceptionHandler.log("onDownloadRemoteOperationFinish success. state="+state);
            switch (state) {
                case STATE_LOADPLAYLIST:
                    // загрузили плейлист
                    MTPlayList playList = null;
                    try {
                        List<LinkedHashMap<String, String>> r = mapper.readValue(new File(dao.getDownFolder().getAbsolutePath(), playListRemotePath), List.class);

                        playList = new MTPlayList();
                        for (LinkedHashMap<String, String> m : r) {
                            MTPlayListRec rec = new MTPlayListRec();
                            rec.setId(Long.parseLong(m.get("id")));
                            rec.setFilename(m.get("filename"));
                            rec.setSize(Long.parseLong(m.get("size")));
                            rec.setType(m.get("type"));
                            rec.setMd5(m.get("md5"));
/*                            LinkedHashMap<String, String> d = m.get("date");
                            if (d != null && d.size() == 2) {

                            }
                            private MTDateRec date = new MTDateRec(); // Период выхода ролика
                            private MTPointRec point = new MTPointRec(); // координаты точки
*/
                            rec.setDuration(Long.parseLong(m.get("duration")));
                            if (m.get("periodicity") != null) {
                                rec.setPeriodicity(Long.parseLong(m.get("periodicity")));
                            }
                            if (m.get("radius") != null) {
                                rec.setRadius(Double.parseDouble(m.get("radius")));
                            }
                            if (m.get("min_count") != null) {
                                rec.setMin_count(Long.parseLong(m.get("min_count")));
                            }
                            if (m.get("max_count") != null) {
                                rec.setMax_count(Long.parseLong(m.get("max_count")));
                            }
                            playList.getPlaylist().add(rec);
                        }
                        //playList = mapper.readValue(new File(dao.getDownFolder().getAbsolutePath(), dao.getRemotePlayListFilePath()), MTPlayList.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        CustomExceptionHandler.logException("playlist convert error ", e);
                    }
                    state = STATE_UNKNOWN;
                    cb.onPlayListLoaded(playList, this);
                    break;
                case STATE_LOADVIDEOFILE:
                    state = STATE_UNKNOWN;
                    // загрузили файл из плейлиста
                    // событие обработчик
                    cb.onVideoFileLoaded(fileToLoad, this);
                    break;
                case STATE_LOADUPDATE:
                    state = STATE_UNKNOWN;
                    // загрузили файл апдейта
                    // событие обработчик
                    cb.onUpdateFileLoaded(this);
                    break;
                case STATE_LOADSIMPLEFILE:
                    state = STATE_UNKNOWN;
                    // загрузили файл
                    // событие обработчик
                    cb.onSimpleFileLoaded(this, new File(dao.getDownFolder().getAbsolutePath(), playListRemotePath));
                    break;
            }
        } else if (operation instanceof UploadRemoteFileOperation ) {
            // загрузили
            if (state == STATE_UPLOADLOG) {
                state = STATE_UNKNOWN;
                cb.onUploadLog(localFileToUpload);
            }
        }
    }

    public void loadVideoFileFromPlayList(MTPlayListRec file) {
        if (state == STATE_UNKNOWN) {
            state = STATE_LOADVIDEOFILE;
            fileToLoad = file;
            CustomExceptionHandler.log("loadVideoFileFromPlayList fileToLoad=" + fileToLoad.getFilename());
            DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(dao.getRemoteVideoDir() + fileToLoad.getFilename(), dao.getDownFolder().getAbsolutePath());
            downloadOperation.addDatatransferProgressListener(this);
            downloadOperation.execute(mClient, this, mHandler);
            CustomExceptionHandler.log("loadVideoFileFromPlayList downloadOperation.executed");
        }
    }

    public void loadFileInfoFromServer() {
        if (state == STATE_UNKNOWN) {
            state = STATE_LOADFILEINFO;
            ReadRemoteFileOperation readInfoOperation = new ReadRemoteFileOperation(playListRemotePath);
            readInfoOperation.execute(mClient, this, mHandler);
            CustomExceptionHandler.log("loadFileInfoFromServer started=" + playListRemotePath);
        }
    }


    public void loadUpdateFromServer() {
        if (state == STATE_UNKNOWN) {
            state = STATE_LOADUPDATE;
            String path = Environment.getExternalStorageDirectory() + "/imtv";
            DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(playListRemotePath, path);
            downloadOperation.addDatatransferProgressListener(this);
            downloadOperation.execute(mClient, this, mHandler);
            CustomExceptionHandler.log("loadUpdateFromServer started=" + playListRemotePath + " to " + path);
        }
    }

    public int getState() {
        return state;
    }

}

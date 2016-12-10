package com.mobile_me.imtv_player.service;

import android.content.Context;

import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pasha on 8/27/16.
 */
public class MTLoaderManager implements IMTCallbackEvent {
    static MTLoaderManager instance;
    List<MTOwnCloudHelper> helpers = new ArrayList<>();

    public static MTLoaderManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new MTLoaderManager(ctx);
        }
        return instance;
    }

    private Context ctx;

    public MTLoaderManager(Context ctx) {
        this.ctx = ctx;
        helpers.add(new MTOwnCloudHelper(Dao.getInstance(ctx).getRemotePlayListFilePath(), ctx, this));
        helpers.add(new MTOwnCloudHelper(Dao.getInstance(ctx).getRemotePlayList2FilePath(), ctx, this));
        CustomExceptionHandler.log("helper created");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Dao.getInstance(MTLoaderManager.this.ctx).getTerminated()) {
                    for (int i = 0; i<Integer.parseInt(MTLoaderManager.this.ctx.getString(R.string.playlists_count)); i++) {
                        helpers.get(i).loadPlayListFromServer();
                    }
                    try {
                        Thread.sleep(Integer.parseInt(MTLoaderManager.this.ctx.getString(R.string.updateplaylist_interval_minutes)) * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
    public void onPlayListLoaded(MTPlayList playListNew, MTOwnCloudHelper ownCloudHelper) {
        CustomExceptionHandler.log("onPlayListLoaded success. playListNew.size="+playListNew.getPlaylist().size());
        //Toast.makeText(this, "Плейлист загружен", Toast.LENGTH_SHORT).show();
        playListNew.setTypePlayList(getPLayListTypeByOwnHandler(ownCloudHelper));
        Dao.getInstance(ctx).getPlayListManagerByType(playListNew.getTypePlayList()).mergeAndSavePlayList(playListNew);
        // запустить загрузку файлов из плейлиста при необходимости
        MTPlayListRec fileToLoad = Dao.getInstance(ctx).getPlayListManagerByType(playListNew.getTypePlayList()).getNextFileToLoad();
        if (fileToLoad != null) {
            helpers.get(playListNew.getTypePlayList() - 1).loadVideoFileFromPlayList(fileToLoad);
        }
    }

    @Override
    public void onVideoFileLoaded(MTPlayListRec file, MTOwnCloudHelper ownCloudHelper) {
        CustomExceptionHandler.log("onVideoFileLoaded start. name="+file.getFilename());
        //Toast.makeText(ctx, "Файл "+file.getFilename()+" загружен", Toast.LENGTH_SHORT).show();
        // найти в списке и проавпдейтить статус
        int typePlayList = getPLayListTypeByOwnHandler(ownCloudHelper);
        Dao.getInstance(ctx).getPlayListManagerByType(typePlayList).setFileStateFlag(file, MTPlayListRec.STATE_UPTODATE);
        // стартовать проверку на загрузку других файлов из плейлиста TODO: тут? или после загрузки плейлиста кажый раз проверять по 1му файлу?
//        doLoadNextVideoFiles();
    }

    @Override
    public void onUpdateFileLoaded(MTOwnCloudHelper ownCloudHelper) {

    }

    @Override
    public void onError(int mode, MTOwnCloudHelper ownCloudHelper, RemoteOperationResult result) {
      // тут ничего не делаем... запустится сам по тамеру в следующий раз
    }

    @Override
    public void onUploadLog(String localFileToUpload) {

    }

    @Override
    public void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file) {

    }

}

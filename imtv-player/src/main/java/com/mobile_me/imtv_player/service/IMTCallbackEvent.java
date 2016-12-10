package com.mobile_me.imtv_player.service;

import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.io.File;

/**
 * Created by pasha on 7/21/16.
 */
public interface IMTCallbackEvent {
    void onPlayListLoaded(MTPlayList playList, MTOwnCloudHelper ownCloudHelper);
    void onVideoFileLoaded(MTPlayListRec file, MTOwnCloudHelper ownCloudHelper);
    void onUpdateFileLoaded(MTOwnCloudHelper ownCloudHelper);
    void onError(int mode, MTOwnCloudHelper ownCloudHelper,  RemoteOperationResult result);
    void onUploadLog(String uploadedLocalFile);
    void onSimpleFileLoaded(MTOwnCloudHelper ownCloudHelper, File file);
}

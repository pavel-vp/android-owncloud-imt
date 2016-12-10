package com.mobile_me.imtv_player.service.tasks;

import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.service.MTPlayListManager;
import com.mobile_me.imtv_player.ui.IMTVApplication;

/**
 * Задача проверяет наличие локального плейлиста и файлов для проигрывания на карте по этому плейлисту.
 * Если плейлиста нет, или ни одного файла доступного нет, то result = false, иначе = true (т.е можно сразу начать проигрывание)
 * Created by pasha on 8/14/16.
 */
public class CheckPlayListLocalTask implements Runnable {

    public boolean result = false;
    public MTPlayList playList = null;
    public int playListType;

    public CheckPlayListLocalTask(int playListType) {
        this.playListType = playListType;
    }

    @Override
    public void run() {
        // Запустить считывание с локальной базы,
        MTPlayList playListNew = Dao.getInstance(IMTVApplication.getInstance()).getPlayListDBHelper().readPlayList(playListType);
        if (playListNew != null && playListNew.getPlaylist().size() > 0) {
            // если плейлист не пуст, пройтись по нему
            // проверить доступность файла по этому пути. Если нет, отметить это в плейлисте
            result = Dao.getInstance(IMTVApplication.getInstance()).getPlayListManagerByType(playListType).checkPlayListFilesOnDisk(playListNew);
            this.playList = playListNew;
        }
    }
}

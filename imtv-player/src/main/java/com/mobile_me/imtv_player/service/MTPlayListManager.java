package com.mobile_me.imtv_player.service;

import android.content.Context;

import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;

import java.io.File;
import java.util.Calendar;

/**
 * Created by pasha on 8/27/16.
 */
public class MTPlayListManager {

    private final MTPlayList playList = new MTPlayList();
    private Context ctx;
    private Dao dao;

    public MTPlayListManager(Context ctx, int typePlayList) {
        this.ctx = ctx;
        this.dao = Dao.getInstance(ctx);
        this.playList.setTypePlayList(typePlayList);
        CustomExceptionHandler.log("MTPlayListManager created for type="+typePlayList);
    }

    public MTPlayList getPlayList() {
        return playList;
    }

    public MTPlayListRec getRandomFile() {
        MTPlayListRec found = null;
        synchronized (playList) {
            int l =(int) (Math.random() * 10);
            for (int i = 0; i < l || found == null; i++) {
                int idx = i >= playList.getPlaylist().size() ? 0 : i;

                if (playList.getPlaylist().get(idx).getState() == MTPlayListRec.STATE_UPTODATE) {
                    found = playList.getPlaylist().get(idx);
                }
            }
        }
        CustomExceptionHandler.log("getRandomFile res="+found);
        return found;
    }

    public MTPlayListRec getNextVideoFileForPlay(boolean forcedPlay) {
        MTPlayListRec found = null;
        synchronized (playList) {
            CustomExceptionHandler.log("getNextVideoFileForPlay start for playList="+playList+", forcedPLay = "+forcedPlay);
            for (MTPlayListRec f : playList.getPlaylist()) {
                if (f.getState() == MTPlayListRec.STATE_UPTODATE) {
                    if (f.getPlayed() == MTPlayListRec.PLAYED_NO) {
                        CustomExceptionHandler.log("getNextVideoFileForPlay setPlayed YES for f="+f);
                        f.setPlayed(MTPlayListRec.PLAYED_YES);
                        Dao.getInstance(ctx).getPlayListDBHelper().updatePlayList(this.playList);
                        CustomExceptionHandler.log("getNextVideoFileForPlay res="+f);
                        return f;
                    }
                }
            }
            if (found == null && forcedPlay) {
                // если не нашли ни одного для проигрывания - сбросить у всех статус и взять первый загруженный
                // это делаем, чтобы проигрывание не останавливалось - проигрываем все по кругу
                // но только если принудительный флаг установлен
                for (MTPlayListRec f : playList.getPlaylist()) {
                    f.setPlayed(MTPlayListRec.PLAYED_NO);
                    CustomExceptionHandler.log("getNextVideoFileForPlay setPlayed NO for f="+f);
                    if (f.getState() == MTPlayListRec.STATE_UPTODATE) {
                        if (found == null) {
                            found = f;
                        }
                    }
                }
            }
            if (found != null) {
                // выставить у найденного флаг проигрывания
                found.setPlayed(MTPlayListRec.PLAYED_YES);
                CustomExceptionHandler.log("getNextVideoFileForPlay setPlayed YES for f="+found);
            }
            Dao.getInstance(ctx).getPlayListDBHelper().updatePlayList(this.playList);
        }
        CustomExceptionHandler.log("getNextVideoFileForPlay res2="+found);
        return found;
    }

    public void setFilePlayFlag(MTPlayListRec rec, int flag) {
        synchronized (playList) {
            rec.setPlayed(flag);
            Dao.getInstance(ctx).getPlayListDBHelper().updatePlayList(this.playList);
        }
    }

    public void setFileStateFlag(MTPlayListRec rec, int flag) {
        synchronized (playList) {
            rec.setState(flag);
            Dao.getInstance(ctx).getPlayListDBHelper().updatePlayList(this.playList);
        }
    }

    public void mergeAndSavePlayList(MTPlayList playListNew) {
        synchronized (playList) {
            CustomExceptionHandler.log("mergeAndSavePlayList playList="+playList + " with new ="+playListNew);
                if (playListNew == null)
                    return;

                    // За основу берем новый плейлист, подтягивая данные по статусам из старого (по ИД)
                    for (MTPlayListRec newR : playListNew.getPlaylist()) {
                        // попробовать найти этот файл в текущем плейлисте
                        MTPlayListRec found = this.playList.searchById(newR.getId());
                        if (found != null) {
                            newR.setState(found.getState());
                            newR.setPlayed(found.getPlayed());
                        }
                    }
/*                    // а также удалить файлы, которых нет уже в новом
                    for (Iterator<MTPlayListRec> iterator = this.playList.getPlaylist().iterator(); iterator.hasNext(); ) {
                        MTPlayListRec pr = iterator.next();
                        // если в новом его нет
                        MTPlayListRec found = playListNew.searchById(pr.getId());
                        if (found == null) {
                            // удалим саму запись, сам видеофайл тоже
                            //log("delete file "+dao.getDownVideoFolder() +"/"+ pr.getFilename());
                            //File f = new File(dao.getDownVideoFolder(), pr.getFilename());
                            //f.delete();
                            iterator.remove();
                        }
                    }*/
                    this.playList.getPlaylist().clear();
                    this.playList.getPlaylist().addAll(playListNew.getPlaylist());
                dao.getPlayListDBHelper().updatePlayList(this.playList);
        }
    }

    public MTPlayListRec getNextFileToLoad() {
        MTPlayListRec fileToLoad = null;
        synchronized (playList) {
                String firstVideoFile = null;
                // по списку файлов пройтись и сравнить их с текущими. Если различаюься - поставить флаг необходимости скачивания
                for (MTPlayListRec f : playList.getPlaylist()) {
                    if (firstVideoFile == null) {
                        firstVideoFile = f.getFilename();
                    }
                    // прочитать локальные данные файла
                    File finfo = new File(dao.getDownVideoFolder(), f.getFilename());
                    //log("loadVideoFileFromPlayList check file info for = "+finfo.getAbsolutePath());
                    if (finfo.exists() && finfo.length() == f.getSize()) { // TODO: потом сделать по MD5
                        f.setState(MTPlayListRec.STATE_UPTODATE);
                       // log("loadVideoFileFromPlayList file state uptodate");
                    } else {
                        f.setState(MTPlayListRec.STATE_NEED_LOAD);
                        if (fileToLoad == null) {
                            fileToLoad = f;
                        }
                        //log("loadVideoFileFromPlayList file state need load");
                    }
                }
                // запустить реальное скачивание первого файла
                if (fileToLoad != null) {
                    //log("loadVideoFileFromPlayList fileToLoad="+fileToLoad.getFilename());
                    fileToLoad.setState(MTPlayListRec.STATE_LOADING);
                    //log("loadVideoFileFromPlayList downloadOperation.executed");
                } else {
                    // все ок, ничего больше не скачиваем
                    //Toast.makeText(ctx,"Все файлы актуальны.", Toast.LENGTH_SHORT).show();
                    //log("loadVideoFileFromPlayList Все файлы актуальны, пробуем запустить первый");
                    //cb.playNextVideoFile();
                }
        }
        CustomExceptionHandler.log("getNextFileToLoad file="+fileToLoad);
        return fileToLoad;
    }

    // Метод проверяет файлы на устройстве, из плейлиста.
    // если файла со статусом "закачан" реально нет по этому пути, то выставить у него статус "незакачан"
    public boolean checkPlayListFilesOnDisk(MTPlayList playListTest) {
        boolean result = false;
        CustomExceptionHandler.log("checkPlayListFilesOnDisk playListTest="+playListTest);
            File path = new File(dao.definePathToVideo(), ctx.getString(R.string.video_dir));
            for (MTPlayListRec plr : playListTest.getPlaylist()) {
                File f = new File(path, plr.getFilename());
                if (plr.getState() == MTPlayListRec.STATE_UPTODATE) {
                    if (!f.exists()) {
                        plr.setState(MTPlayListRec.STATE_NEED_LOAD);
                    } else {
                        result = true; // есть хоть один
                    }
                }
            }
        CustomExceptionHandler.log("checkPlayListFilesOnDisk res="+result);
        return result;
    }

}

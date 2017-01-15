package com.mobile_me.imtv_player.service;

import android.content.Context;

import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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

    /**
     * Верхняя процедура возвращает файл для проигрывания
     * @param forcedPlay
     * @return
     */
    public MTPlayListRec getNextVideoFileForPlay(boolean forcedPlay) {
        MTPlayListRec found = null;
        synchronized (playList) {
            CustomExceptionHandler.log("getNextVideoFileForPlay start for playList="+playList+", forcedPLay = "+forcedPlay);
            // ЭТАП 1. поиск файла для проигрывания в этом плейлисте
            found = getNextVideoFileForPlayInternal();
            // ЭТАП 2. не нашли - ищем любой форсированно
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

/*    static class MTStatInfo {
        int durationFreeSec = 0; // число секунд воспроиведения НЕКОММ видео
        Map<Long, Integer> cntMap = new HashMap<>();
        Map<Long, Long> durationMap = new HashMap<>();

    }
    private MTStatInfo getStatInfo(int lastMinutes) {
        MTStatInfo res = new MTStatInfo();
        // прочитаем данные статистики за последние 30 минут
        for (MTPlayListRec r : dao.getmStatisticDBHelper().readStatOnLastNMins(lastMinutes)) {
            if (MTPlayListRec.TYPE_FREE.equals(r.getType())) {
                res.durationFreeSec += r.getDuration();
            }
            Integer cnt =res.cntMap.get(r.getId());
            res.cntMap.put(r.getId(), cnt == null ? 1 : cnt + 1);
            Long duration =res.durationMap.get(r.getId());
            res.durationMap.put(r.getId(), duration == null ? 1 : duration + r.getDuration());

        }
        return res;
    }
*/

    static class MTCommercialInfo {
        MTPlayListRec mtPlayListRec;
        double planQty = 0;
        double factQty = 0;
        double priority = 0;

        @Override
        public String toString() {
            return "MTCommercialInfo{" +
                    " planQty=" + planQty +
                    ", factQty=" + factQty +
                    ", priority=" + priority +
                    ", mtPlayListRec=" + mtPlayListRec +
                    '}';
        }
    }

    // Метод возвращает очередь по приоритетам, в которой коммерческие ролики отсортированы по
    // значению (план-факт) воспроизведения за это число минут. С наибольшим приоритетом - вверху
    private PriorityQueue<MTCommercialInfo> getCommercialPQ(int lastMinutes, List<MTPlayListRec> statList) {
        PriorityQueue<MTCommercialInfo> q = new PriorityQueue<>(100, new Comparator<MTCommercialInfo>() {
            @Override
            public int compare(MTCommercialInfo lhs, MTCommercialInfo rhs) {
                if (lhs.priority > rhs.priority) return -1;
                if (lhs.priority < rhs.priority) return 1;
                return 0;
            }
          });
        // соберем сперва в список все коммерческие ролики
        Map<Long, MTCommercialInfo> commMap = new HashMap<>();
        for (MTPlayListRec rc : playList.getPlaylist()) {
            if (rc.getState() == MTPlayListRec.STATE_UPTODATE &&
                    MTPlayListRec.TYPE_COMMERCIAL.equals(rc.getType())) {
                MTCommercialInfo ci = new MTCommercialInfo();
                ci.mtPlayListRec = rc;
                ci.planQty = (double)lastMinutes / (double)rc.getPeriodicity();
                ci.priority = ci.planQty;
                commMap.put(rc.getId(), ci);
            }
        }
        // пройтись по факту за последние минуты и расчитаем приоритет
        for (MTPlayListRec r : statList) {
            MTPlayListRec rc = playList.searchById(r.getId());
            if (rc != null &&
                    rc.getState() == MTPlayListRec.STATE_UPTODATE &&
                    MTPlayListRec.TYPE_COMMERCIAL.equals(r.getType())) {
                MTCommercialInfo ci = commMap.get(r.getId());
                if (ci == null) {
                    ci = new MTCommercialInfo();
                    ci.mtPlayListRec = rc;
                    commMap.put(r.getId(), ci);
                }
                ci.factQty = ci.factQty + 1;
                ci.priority = ci.planQty - ci.factQty;
            }
        }
        // из мапы перенесем в очередь
        for (MTCommercialInfo ci : commMap.values()) {
            q.add(ci);
        }
        return q;
    }

    static class MTFreeInfo {
        MTPlayListRec mtPlayListRec;
        double priority = 0;

        @Override
        public String toString() {
            return "MTFreeInfo{" +
                    "priority=" + priority +
                    ", mtPlayListRec=" + mtPlayListRec +
                    '}';
        }
    }
    // Метод возвращает очередь по приоритетам, в которой некоммерческие ролики отсортированы по
    // значению (факт) воспроизведения за это число минут. С наименьшим количеством воспроизведения - вверху
    private PriorityQueue<MTFreeInfo> getFreePQ(int lastMinutes, List<MTPlayListRec> statList) {
        PriorityQueue<MTFreeInfo> q = new PriorityQueue<>(100, new Comparator<MTFreeInfo>() {
            @Override
            public int compare(MTFreeInfo lhs, MTFreeInfo rhs) {
                if (lhs.priority < rhs.priority) return -1;
                if (lhs.priority > rhs.priority) return 1;
                return 0;
            }
        });
        // соберем сперва в список все некоммерческие ролики
        Map<Long, MTFreeInfo> freeMap = new HashMap<>();
        for (MTPlayListRec rc : playList.getPlaylist()) {
            if (rc.getState() == MTPlayListRec.STATE_UPTODATE &&
                    MTPlayListRec.TYPE_FREE.equals(rc.getType())) {
                MTFreeInfo ci = new MTFreeInfo();
                ci.mtPlayListRec = rc;
                freeMap.put(rc.getId(), ci);
            }
        }
        // пройтись по факту за последние минуты и расчитаем приоритет
        for (MTPlayListRec r : statList) {
            MTPlayListRec rc = playList.searchById(r.getId());
            if (rc != null &&
                    rc.getState() == MTPlayListRec.STATE_UPTODATE &&
                    MTPlayListRec.TYPE_FREE.equals(r.getType())) {
                MTFreeInfo ci = freeMap.get(r.getId());
                if (ci == null) {
                    ci = new MTFreeInfo();
                    ci.mtPlayListRec = rc;
                    freeMap.put(r.getId(), ci);
                }
                ci.priority = ci.priority + 1;
            }
        }
        // из мапы перенесем в очередь
        for (MTFreeInfo ci : freeMap.values()) {
            q.add(ci);
        }
        return q;
    }

    /**
     * Функция реализует основной алгоритм поиска файла для проигрывания
     * @return
     */
    private MTPlayListRec getNextVideoFileForPlayInternal() {
        int lastMinutes = 30; // FIXME: в настройки
        CustomExceptionHandler.log("start calc next file");
        // на входе - статистика за последние 30 мин, текущий плейлист с актуальными файлами для проигрывания
        List<MTPlayListRec> statList = dao.getmStatisticDBHelper().readStatOnLastNMins(lastMinutes);

        // TODO: GPS

        // TODO: логировать данные о сохраненных проигрываниях (на основании чего считаем), и выбранного факта, чтобы потом на основании лога можно было понять почему проигралась эта запись
        CustomExceptionHandler.log("statList.size="+statList.size());

        // КОММЕРЧЕСКОЕ
        // получим очередь по приоритетам с воспроизведением коммерческого.
        PriorityQueue<MTCommercialInfo> q =getCommercialPQ(lastMinutes, statList);
        // выведем очередь в лог
        CustomExceptionHandler.log("commercial queue=");
        for (MTCommercialInfo c : q) {
            CustomExceptionHandler.log("c="+c);
        }
        MTCommercialInfo resComm = q.poll();
        CustomExceptionHandler.log("resComm="+resComm);
        // Если есть еще что проигрывать в коммерческом
        if (resComm != null && resComm.priority > 0) {
            // проигрываем его
            return resComm.mtPlayListRec;
        }

        // Нет ничего что проигрывать в коммерческой очереди (либо нет коммерческой вообще)
        // НЕКОММЕРЧЕСКОЕ
        // получим очередь по приоритетам для некоммерческого
        PriorityQueue<MTFreeInfo> qf = getFreePQ(lastMinutes, statList);
        // выведем очередь в лог
        CustomExceptionHandler.log("free queue=");
        for (MTFreeInfo c : qf) {
            CustomExceptionHandler.log("c="+c);
        }
        MTFreeInfo fi = qf.poll();
        CustomExceptionHandler.log("fi="+fi);
        // Если есть что-то проигрывать в некоммерческом
        if (fi != null ) {
            // проигрываем его
            return fi.mtPlayListRec;
        }

        CustomExceptionHandler.log("nothing to play");
        // Нет ничего что надо проигрывать по плану и в некоммерческом.
        // Если вообще есть что-то в коммерческой очереди
        if (resComm != null) {
            return resComm.mtPlayListRec;
        }
        CustomExceptionHandler.log("nothing to play at all");
        // Если вообще есть что-то в некоммерческой очереди
        if (fi != null) {
            return fi.mtPlayListRec;
        }
        CustomExceptionHandler.log("nothing to play - bad");

        return null; // ничего нет !
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

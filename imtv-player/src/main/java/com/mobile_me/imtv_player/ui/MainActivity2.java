package com.mobile_me.imtv_player.ui;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.VideoView;

import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.service.MTLoaderManager;
import com.mobile_me.imtv_player.service.MTPlayListManager;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.mobile_me.imtv_player.util.RootUtils;
import com.stericson.rootshell.RootShell;
import com.stericson.roottools.RootTools;

import java.util.Calendar;

/**
 * Created by pasha on 10/12/16.
 */
public class MainActivity2  extends Activity implements SensorEventListener, LocationListener {

    VideoView vw1;
    VideoView vw2;
    MTLoaderManager loaderManager;
    Dao dao;
    private boolean isActive = false;
    private long lastStartPlayFile;

    boolean isInPreparing = false;
    boolean isInPreparing2 = false;
    private boolean is2Players = false;
    private Handler handler = new Handler();
    private volatile Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());

        lastStartPlayFile = Calendar.getInstance().getTimeInMillis();
        SensorManager mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor TempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorManager.registerListener(this, TempSensor, SensorManager.SENSOR_DELAY_NORMAL);

        vw1 = (VideoView) findViewById(R.id.videoView);
        //vw2 = (VideoView) findViewById(R.id.videoView2);

        loaderManager = MTLoaderManager.getInstance(this);
        dao = Dao.getInstance(this);
        vw1.setMediaController(null);
        vw1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                CustomExceptionHandler.log("onCompletion 1");
                playNextVideoFile(vw1);
            }
        });
        vw1.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                CustomExceptionHandler.log("onError 1, what="+what + ", extra="+extra);
                playNextVideoFile(vw1);
                return true;
            }
        });
/*        vw2.setMediaController(null);
        vw2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                CustomExceptionHandler.log("onCompletion 2");
                playNextVideoFile(vw2);
            }
        });
        */
        is2Players = Integer.parseInt(getResources().getString(R.string.playlists_count)) > 1;

        if (is2Players) {
            // установить размеры плееров (первый - 4 на 3, остальной - оставшаяся ширина
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int height = displaymetrics.heightPixels;
            int width = height * 4 / 3;
            ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height-1);
            vw1.setLayoutParams(layoutParams);
            //holder.setFixedSize(width, height-1);

            vw2.setVisibility(View.VISIBLE);
            layoutParams = new LinearLayout.LayoutParams(displaymetrics.widthPixels - width, height-1);
            vw2.setLayoutParams(layoutParams);
            //holder2.setFixedSize(displaymetrics.widthPixels - width, height-1);
        } else {
        //    ViewGroup.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        //    vw1.setLayoutParams(layoutParams);
/*            layoutParams = new LinearLayout.LayoutParams(0, 0, 0);
            vw2.setLayoutParams(layoutParams);
            vw2.setVisibility(View.GONE);*/
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!dao.getTerminated()) {
                    Calendar calNow = Calendar.getInstance();
                    calNow.add(Calendar.SECOND, 0-Integer.parseInt(getString(R.string.good_location_interval_seconds)));
                    Calendar calLoc = Calendar.getInstance();
                    calLoc.setTime(calLoc.getTime());
                    if (currentLocation != null && calNow.after(calLoc)) {
                        currentLocation = null;
                    }
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    protected void onStart() {
        CustomExceptionHandler.log("onStart");
        super.onStart();
        isActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                CustomExceptionHandler.log("hang checking isactive="+isActive+", dao.getTerminated()="+dao.getTerminated());
                while (isActive && !dao.getTerminated()) {
                    // проверить на то что завис плеер (старт предыдущего проигрывания)
                    long now = Calendar.getInstance().getTimeInMillis();
                    CustomExceptionHandler.log("hang checking now-last="+(now - lastStartPlayFile));
                    if ((now - lastStartPlayFile) > (1000 * 60 * Long.parseLong(getString(R.string.hang_interval_minutes))) ) {
                        CustomExceptionHandler.log("hang detected");
                        dao.setTerminated(true);
                        break;
                    }
                    try {
                        Thread.sleep(60000);
//                        Thread.sleep(6);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (dao.getTerminated()) {
                    CustomExceptionHandler.log("finish by hang");
                    //MainActivity2.this.finish();
                    //System.exit(0);

                    try {
                        String command;
                        command = "reboot";
                        Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                        int res = 0;
                        res = proc.waitFor();
                        CustomExceptionHandler.log("res procReboot=" + res);
                    } catch (Exception e) {
                        CustomExceptionHandler.logException("error reboot - ", e);
                    }

                }
            }
        }).start();

        // запуск проигрывания сразу
        playNextVideoFile(vw1);
        if (is2Players) {
            playNextVideoFile(vw2);
        }

    }

    @Override
    protected void onStop() {
        CustomExceptionHandler.log("onStop");
        super.onStop();
        isActive = false;
        isInPreparing = false;
        isInPreparing2 = false;
        vw1.stopPlayback();
        if (is2Players) {
            vw2.stopPlayback();
        }
    }

    public void playNextVideoFile(VideoView vw) {
        lastStartPlayFile = Calendar.getInstance().getTimeInMillis();
        int type = (vw == vw1) ? MTPlayList.TYPEPLAYLIST_1 : MTPlayList.TYPEPLAYLIST_2;
        CustomExceptionHandler.log("playNextVideoFile started. type="+type+",lastStartPlayFile="+lastStartPlayFile);
        logMemory();
        if (vw.isPlaying() || (type == MTPlayList.TYPEPLAYLIST_1 && isInPreparing) || (type == MTPlayList.TYPEPLAYLIST_2 && isInPreparing2) || !isActive) {
            CustomExceptionHandler.log("playNextVideoFile playing exit, type="+type);
            return;
        }

        String filePathToPlay = null;

        if (false) {
            filePathToPlay.substring(1);
        }

        boolean forcedPlay = false;
        MTPlayListManager playListManager = Dao.getInstance(this).getPlayListManagerByType(type );
        MTPlayListRec found = null;
        if (type == MTPlayList.TYPEPLAYLIST_1) {
            // взять из плейлиста следующий непроигранный
            if (playListManager.getPlayList() != null) {
                found = playListManager.getNextVideoFileForPlay(forcedPlay);
            }
            // если непроигранного нет - значит все проиграли, запустим поиск с принудительнм возвращением хотя бы одного,
            // при этом по логике отбора файла - он должен вернуть хотя бы один, и возможно сбросить все состояния проигрывания.
            if (found == null) {
                forcedPlay = true;
                found = playListManager.getNextVideoFileForPlay(forcedPlay);
            }
            filePathToPlay = dao.getDownVideoFolder() + found.getFilename();
        } else {
            found = playListManager.getRandomFile();
            filePathToPlay = dao.getDownVideoFolder() + found.getFilename();
        }
        if (filePathToPlay != null && isActive && found != null) {
            // запустить проигрывание этого файла
            CustomExceptionHandler.log("playList start playing, type="+type+", filePathToPlay="+filePathToPlay);
            vw.setVideoPath(filePathToPlay);
            vw.start();
            dao.getmStatisticDBHelper().addStat(found, currentLocation);
        }
        if (forcedPlay) {
            // запустить загрузку плейлиста TODO: не надо же уже?
            //helper.loadPlayListFromServer();
        }
        CustomExceptionHandler.log("playNextVideoFile finished. type="+type);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float temp = event.values[0];
        CustomExceptionHandler.log("sensor event="+event+", sensor="+event.sensor.getName()+ ", value="+temp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void logMemory() {
        Runtime info = Runtime.getRuntime();
        long freeSize = info.freeMemory();
        long totalSize = info.totalMemory();
        long maxSize = info.maxMemory();
        CustomExceptionHandler.log("memory: freeSize="+freeSize+", totalSize="+totalSize+ ", maxSize="+maxSize);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

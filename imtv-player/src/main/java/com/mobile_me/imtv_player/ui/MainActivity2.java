package com.mobile_me.imtv_player.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.VideoView;

import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.mobile_me.imtv_player.R;
import com.mobile_me.imtv_player.dao.Dao;
import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.service.MTLoaderManager;
import com.mobile_me.imtv_player.service.MTPlayListManager;
import com.mobile_me.imtv_player.service.StatUpload;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;
import com.mobile_me.imtv_player.util.RootUtils;
import com.stericson.rootshell.RootShell;
import com.stericson.roottools.RootTools;

import java.io.File;
import java.util.Calendar;

/**
 * Created by pasha on 10/12/16.
 */
public class MainActivity2  extends Activity implements SensorEventListener, LocationListener {

//    VideoView vw1;
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

    private SimpleExoPlayerView exoPlayerView;
    private SimpleExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler.getLog());

        lastStartPlayFile = Calendar.getInstance().getTimeInMillis();
        SensorManager mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor TempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorManager.registerListener(this, TempSensor, SensorManager.SENSOR_DELAY_NORMAL);

        exoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoplayer);
        exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);

        exoPlayerView.hideController();
        exoPlayerView.setControllerVisibilityListener(new PlaybackControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int i) {
                if(i == 0) {
                    exoPlayerView.hideController();
                }
            }
        });

        //vw2 = (VideoView) findViewById(R.id.videoView2);

        loaderManager = MTLoaderManager.getInstance(this);
        dao = Dao.getInstance(this);


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
            //vw1.setLayoutParams(layoutParams);
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
        StatUpload.getInstance(dao).startUploadStat();

    }

    Player.EventListener exoPlayerListener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            CustomExceptionHandler.log("onTracksChanged");
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            CustomExceptionHandler.log("onLoadingChanged "+isLoading);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            CustomExceptionHandler.log("onPlayerStateChanged "+playWhenReady+","+playbackState);
            if (playbackState == Player.STATE_ENDED) {
                playNextVideoFile();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            CustomExceptionHandler.logException("onPlayerError", error);
            playNextVideoFile();
        }

        @Override
        public void onPositionDiscontinuity() {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }
    };

    private void initializePlayerAndPlayFile(String filePath) {
        releasePlayer();
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(), new DefaultLoadControl());
        exoPlayer.addListener(exoPlayerListener);
        exoPlayerView.setPlayer(exoPlayer);
        exoPlayer.setPlayWhenReady(true);

        Uri uri = Uri.fromFile(new File(filePath));
        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };
        MediaSource audioSource = new ExtractorMediaSource(fileDataSource.getUri(),
                factory, new DefaultExtractorsFactory(), null, null);

        exoPlayer.prepare(audioSource);
     //   exoPlayer.seekTo(0);

    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource(uri,
                new DefaultHttpDataSourceFactory("ua"),
                new DefaultExtractorsFactory(), null, null);
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        exoPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSystemUi();
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
        playNextVideoFile();
        if (is2Players) {
            playNextVideoFile();
        }

    }

    @Override
    protected void onStop() {
        CustomExceptionHandler.log("onStop");
        super.onStop();
        isActive = false;
        isInPreparing = false;
        isInPreparing2 = false;
        releasePlayer();
        if (is2Players) {
            vw2.stopPlayback();
        }
    }

    public void playNextVideoFile() {
        lastStartPlayFile = Calendar.getInstance().getTimeInMillis();
        int type = MTPlayList.TYPEPLAYLIST_1 ;
        CustomExceptionHandler.log("playNextVideoFile started. type="+type+",lastStartPlayFile="+lastStartPlayFile);
        logMemory();
        if (exoPlayer != null) {
            releasePlayer();
//            CustomExceptionHandler.log("playNextVideoFile playing exit, type="+type);
//            return;
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
            initializePlayerAndPlayFile(filePathToPlay);
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

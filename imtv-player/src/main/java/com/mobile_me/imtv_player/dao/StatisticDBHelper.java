package com.mobile_me.imtv_player.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by pasha on 20.12.16.
 */
public class StatisticDBHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "playstatdata";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");

    private static final String ID = "id";
    private static final String DT = "dt";
    private static final String DTDAYHOUR = "dtdayhour";
    private static final String DURATION = "duration";
    private static final String POINT_LAT = "point_lat";
    private static final String POINT_LON = "point_lon";
    private static final String TYPE = "typelist";

    Context context;
    public static final String CREATE_TABLE = "create table " + TABLE_NAME + " ( "
            + ID + " integer, "
            + DT + " long, "
            + DTDAYHOUR + " long, "
            + DURATION + " int, "
            + POINT_LAT + " double, "
            + POINT_LON + " double, "
            + TYPE + " text "
            + ");";


    public StatisticDBHelper(Context context) {
        super(context, Dao.DB_NAME, null, Dao.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(StatisticDBHelper.CREATE_TABLE);
        db.execSQL(PlayListDBHelper.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Добавляем запись статистики
    public void addStat(MTPlayListRec recExt, Location loc) {
        CustomExceptionHandler.log("write stat  recExt="+recExt);
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(ID, recExt.getId());
            cv.put(DURATION, recExt.getDuration());
            Calendar cal = Calendar.getInstance();
            cv.put(DT, cal.getTimeInMillis());
            cv.put(DTDAYHOUR, Long.parseLong(sdf.format(cal.getTime())));
            if (loc != null) {
                cv.put(POINT_LAT, loc.getLatitude());
                cv.put(POINT_LON, loc.getLatitude());
            }
            cv.put(TYPE, recExt.getType());
            db.insert(TABLE_NAME, null, cv);
        } catch (Exception e) {
            CustomExceptionHandler.logException("write stat rec error ", e);
            e.printStackTrace();
        }
        CustomExceptionHandler.log("write stat rec end ");
    }

    // Метод для получения данных из статистики:
    // - читает данные по статистике за указанные последние N минут
    public List<MTPlayListRec> readStatOnLastNMins(int lastMinutes) {
        // попробовать прочитать локально сохраненный плейлист.
        CustomExceptionHandler.log("start read last minutes: "+ lastMinutes);
        long timeStart = Calendar.getInstance().getTimeInMillis() - (lastMinutes * 60 * 1000);
        List<MTPlayListRec> res = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("select * from "+ TABLE_NAME + " where "+ DT + " >= ?  order by " + DT, new String[] { String.valueOf(timeStart)});
        if (cur != null ) {
            if (cur.moveToFirst()) {
                do {
                    MTPlayListRec rec = new MTPlayListRec();
                    rec.setId(cur.getLong(cur.getColumnIndex(ID)));
                    rec.setDuration(cur.getLong(cur.getColumnIndex(DURATION)));
                    rec.setType(cur.getString(cur.getColumnIndex(TYPE)));
                    rec.setFilename(cur.getString(cur.getColumnIndex(DTDAYHOUR)));
                    res.add(rec);
                    CustomExceptionHandler.log("statrec="+rec);
                }  while (cur.moveToNext()) ;
            }
            cur.close();
        }
        CustomExceptionHandler.log("end read ");
        return res;
    }


}

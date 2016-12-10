package com.mobile_me.imtv_player.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mobile_me.imtv_player.model.MTPlayList;
import com.mobile_me.imtv_player.model.MTPlayListRec;
import com.mobile_me.imtv_player.util.CustomExceptionHandler;

/**
 * Created by pasha on 8/14/16.
 */
public class PlayListDBHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "playlist";

    private static final String ID = "id";
    private static final String FILENAME = "filename";
    private static final String SIZE = "size";
    private static final String DURATION = "duration";
    private static final String STATE = "state";
    private static final String PLAYED = "played";
    private static final String NUMORD = "numord";
    private static final String UPDATED = "updated";
    private static final String TYPEPLAYLIST = "typeplaylist";

    Context context;
    public static final String CREATE_TABLE = "create table " + TABLE_NAME + " ( "
            + ID + " integer, "
            + FILENAME + " text, "
            + SIZE + " int, "
            + DURATION + " int, "
            + STATE + " int, "
            + PLAYED + " int, "
            + NUMORD + " int, "
            + UPDATED + " int, "
            + TYPEPLAYLIST + " int "
            + ");";


    public PlayListDBHelper(Context context) {
        super(context, Dao.DB_NAME, null, Dao.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public MTPlayList readPlayList(int typePlayList) {
        // попробовать прочитать локально сохраненный плейлист.
        CustomExceptionHandler.log("start read "+ typePlayList);
        MTPlayList list = new MTPlayList();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("select * from "+ TABLE_NAME + " where "+ UPDATED + " = ? and " + TYPEPLAYLIST + " = ? order by " + NUMORD, new String[] { String.valueOf(1), String.valueOf(typePlayList)});
        if (cur != null ) {
            if (cur.moveToFirst()) {
                do {
                    MTPlayListRec rec = new MTPlayListRec();
                    rec.setId(cur.getLong(cur.getColumnIndex(ID)));
                    rec.setFilename(cur.getString(cur.getColumnIndex(FILENAME)));
                    rec.setSize(cur.getLong(cur.getColumnIndex(SIZE)));
                    rec.setDuration(cur.getLong(cur.getColumnIndex(DURATION)));
                    rec.setState(cur.getInt(cur.getColumnIndex(STATE)));
                    rec.setPlayed(cur.getInt(cur.getColumnIndex(PLAYED)));
                    list.getPlaylist().add(rec);
                }  while (cur.moveToNext()) ;
            }
            cur.close();
        }
        CustomExceptionHandler.log("end read "+ list);
        return list;
    }

    public void updatePlayList(MTPlayList playList) {
        int typePlayList = playList.getTypePlayList();
        CustomExceptionHandler.log("write playList = "+ playList);
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("update " + TABLE_NAME + " set " + UPDATED + " = 0 where "+TYPEPLAYLIST+" ="+typePlayList);
            int numord = 1;
            for (MTPlayListRec plr : playList.getPlaylist()) {
                ContentValues cv = new ContentValues();
                cv.put(ID, plr.getId());
                cv.put(FILENAME, plr.getFilename());
                cv.put(SIZE, plr.getSize());
                cv.put(DURATION, plr.getDuration());
                cv.put(STATE, plr.getState());
                cv.put(PLAYED, plr.getPlayed());
                cv.put(NUMORD, numord);
                cv.put(UPDATED, 1);
                cv.put(TYPEPLAYLIST, typePlayList);
                // если такой файл уже есть - перезаписать его
                Cursor cur = db.rawQuery("select * from "+ TABLE_NAME+" where "+ID + " = ? and " + TYPEPLAYLIST + " = ?", new String[]{String.valueOf(plr.getId()), String.valueOf(typePlayList)});
                if (cur != null) {
                    if (cur.getCount() > 0) {
                        db.update(TABLE_NAME, cv, ID + " = ? and "+TYPEPLAYLIST+" = ?", new String[]{String.valueOf(plr.getId()), String.valueOf(typePlayList)});
                    } else {
                        db.insert(TABLE_NAME, null, cv);
                    }
                    cur.close();
                }
                //db.close();
                numord++;
            }
        } catch (Exception e) {
            CustomExceptionHandler.logException("write playList error ", e);
            e.printStackTrace();
        }
        CustomExceptionHandler.log("write playList end "+ playList.getPlaylist().size());
    }
}

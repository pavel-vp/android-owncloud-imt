package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by pasha on 24.12.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTPlayListRec implements Serializable {

    public static final String TYPE_COMMERCIAL = "COMMERCE";
    public static final String TYPE_FREE = "FREE";
    public static final String TYPE_GEO = "GEO";

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_NEED_LOAD = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_UPTODATE = 3;

    public static final int PLAYED_NO = 0;
    public static final int PLAYED_YES = 1;

    // Общее
    private Long id;
    private String filename; // ИмяФайла
    private Long duration = Long.valueOf(0); // Время ролика: 23сек.
    private Long size = Long.valueOf(0); // размер в байтах
    private String type;
    private MTDateRec date = new MTDateRec(); // Период выхода ролика
    private String md5; // md5(“ИмяФайла” + “Время ролика”)

    // Для коммерческого
    private Long periodicity = Long.valueOf(0); // Периодичность: раз в 10 мин

    // GPS таргетированные
    private MTPointRec point = new MTPointRec(); // координаты точки
    private Double radius = Double.valueOf(0); // Радиус (метры)
    private Long max_count = Long.valueOf(0); // Макс. Кол. Воспроизведений за раз в геолокации
    private Long min_count = Long.valueOf(0); // Мин. обязательное кол. Раз в геолокации

    private transient int state = STATE_UNKNOWN;
    private transient int played = PLAYED_NO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MTDateRec getDate() {
        return date;
    }

    public void setDate(MTDateRec date) {
        this.date = date;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Long getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Long periodicity) {
        this.periodicity = periodicity;
    }

    public MTPointRec getPoint() {
        return point;
    }

    public void setPoint(MTPointRec point) {
        this.point = point;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public Long getMax_count() {
        return max_count;
    }

    public void setMax_count(Long max_count) {
        this.max_count = max_count;
    }

    public Long getMin_count() {
        return min_count;
    }

    public void setMin_count(Long min_count) {
        this.min_count = min_count;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getPlayed() {
        return played;
    }

    public void setPlayed(int played) {
        this.played = played;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "MTPlayListRec{" +
                "type='" + type + '\'' +
                ", id=" + id +
                ", filename='" + filename + '\'' +
                ", duration=" + duration +
                ", size=" + size +
                '}';
    }
}


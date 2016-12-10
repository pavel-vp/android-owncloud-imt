package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by pasha on 7/21/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTPlayListRec implements Serializable {
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_NEED_LOAD = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_UPTODATE = 3;

    public static final int PLAYED_NO = 0;
    public static final int PLAYED_YES = 1;

    Long id;
    String filename;
    Long size;
    Long duration;
    int state = STATE_UNKNOWN;
    int played = PLAYED_NO;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
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

/*
    @Override
    public String toString() {
        return new StringBuilder()
                .append("MTPlayListRec{")
                .append("id=")
                .append(id)
                .append(", filename='")
                .append(filename)
                .append(", size=")
                .append(size)
                .append(", duration=")
                .append(duration)
                .append(", state=")
                .append(state)
                .append(", played=")
                .append(played)
                .append("}" ).toString();
    }
    */
}

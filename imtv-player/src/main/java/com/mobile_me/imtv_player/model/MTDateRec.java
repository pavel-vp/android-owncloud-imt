package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by pasha on 24.12.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTDateRec implements Serializable {

    private String from;
    private String to;

    public MTDateRec() {}

    public MTDateRec(String from1, String to1) {
        this.from = from1;
        this.to = to1;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }


    @Override
    public String toString() {
        return "MTDateRec{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}

package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by pasha on 24.12.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTPointRec implements Serializable {

    private double x;
    private double y;

    public MTPointRec() {}

    public MTPointRec(double x1, double y1) {
        this.x = x1;
        this.y = y1;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    @Override
    public String toString() {
        return "MTPointRec{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}

package com.mobile_me.imtv_player.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pasha on 7/21/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MTPlayList implements Serializable {

    public static final int TYPEPLAYLIST_1 = 1;
    public static final int TYPEPLAYLIST_2 = 2;
    private int typePlayList;

    public MTPlayList() {}

    private List<MTPlayListRec> playlist = new ArrayList<>();

    public List<MTPlayListRec> getPlaylist() {
        return playlist;
    }

    public MTPlayListRec searchById(Long id) {
        if (playlist.size() > 0) {
            for (MTPlayListRec r : playlist) {
                    if (r.getId().equals(id)) {
                        return r;
                    }
            }
        }
        return null;
    }

    public int getTypePlayList() {
        return typePlayList;
    }

    public void setTypePlayList(int typePlayList) {
        this.typePlayList = typePlayList;
    }

}

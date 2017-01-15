package com.mobile_me.imtv_player.service.tasks;

import com.mobile_me.imtv_player.util.CustomExceptionHandler;

/**
 * Created by pasha on 08.01.17.
 */
public class CheckForSettingsLocalTask implements Runnable {

    @Override
    public void run() {
        CustomExceptionHandler.log("waiting for loaded setuprec...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

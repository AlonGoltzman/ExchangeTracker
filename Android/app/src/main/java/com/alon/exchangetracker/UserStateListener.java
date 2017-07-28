package com.alon.exchangetracker;

import java.util.TimerTask;

/**
 * Created by Alon on 6/28/2017.
 */

interface UserStateListener {

    void invalidUID(TimerTask executable);

}

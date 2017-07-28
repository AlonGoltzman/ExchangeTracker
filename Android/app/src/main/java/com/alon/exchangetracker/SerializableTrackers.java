package com.alon.exchangetracker;

import java.io.Serializable;
import java.util.ArrayList;

class SerializableTrackers implements Serializable {
    private ArrayList<Tracker> trackers;
    private double version = 0;

    SerializableTrackers(Trackers trackersObj) {
        this.trackers = trackersObj.getTrackers();
        version = trackersObj.getVersion();
    }

    ArrayList<Tracker> getTrackers() {
        return trackers;
    }

    double getVersion() {
        return version;
    }
}


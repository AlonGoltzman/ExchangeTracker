package com.alon.exchangetrackerserver;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alon on 6/23/2017.
 */
public class ExchangeTrackerTrackers {

    private static HashMap<String, ExchangeTrackerInformation> trackersPerUID;

    static {
        if (trackersPerUID == null)
            trackersPerUID = new HashMap<>();
    }

    static boolean shouldPullTracker(String uid, double clientVersion) {
        ExchangeTrackerInformation tracker = trackersPerUID.get(uid);
        if (tracker == null) {
            trackersPerUID.put(uid, new ExchangeTrackerInformation());
            return false;
        }
        return tracker.getVersion() != clientVersion;
    }

    static ExchangeTrackerInformation getTracker(String uid) {
        return trackersPerUID.get(uid);
    }

    static boolean insertNewTracker(String uid, JSONArray trackerInformation) {
        if (!trackersPerUID.containsKey(uid)) {
            trackersPerUID.put(uid, new ExchangeTrackerInformation(trackerInformation));
            return true;
        } else {
            ExchangeTrackerInformation info = trackersPerUID.get(uid);
            return info.insert(trackerInformation);
        }
    }

    static boolean updateTracker(String uid, JSONArray updatedInfo) {
        if (!trackersPerUID.containsKey(uid))
            return false;
        else {
            ExchangeTrackerInformation info = trackersPerUID.get(uid);
            return info.update(updatedInfo);
        }
    }

    static void deleteAllTrackers(String uid) {
        if (trackersPerUID.containsKey(uid))
            trackersPerUID.remove(uid);
    }

    static boolean deleteTracker(String uid, String base) {
        return trackersPerUID.containsKey(uid) && trackersPerUID.get(uid).delete(base);
    }

    static boolean deleteTrackerForForeignCurrency(String uid, String base, String foreign) {
        return trackersPerUID.containsKey(uid) && trackersPerUID.get(uid).delete(base, foreign);
    }

    static Set<String> getTackerBases() {
        Set<String> bases = new HashSet<>();
        for (ExchangeTrackerInformation tracker : trackersPerUID.values())
            bases.addAll(tracker.getAllBases());
        return bases;
    }

    static Set<String> getForeignsForBase(String base) {
        Set<String> foreigns = new HashSet<>();
        for (ExchangeTrackerInformation tracker : trackersPerUID.values())
            foreigns.addAll(tracker.getAllForeign(base));
        return foreigns;
    }

    static boolean hasTrackers() {
        return trackersPerUID.values().size() > 0;
    }
}

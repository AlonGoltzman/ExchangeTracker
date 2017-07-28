package com.alon.exchangetrackerserver;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.logging.Logger;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.*;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.Log;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.stackTraceAsString;

/**
 * Created by Alon on 6/23/2017.
 */
public class ExchangeTrackerInformation {

    private final double INC = 0.1;

    private ArrayList<Tracker> trackers;
    private double version = 0;

    ExchangeTrackerInformation() {
        trackers = new ArrayList<>();
    }

    ExchangeTrackerInformation(JSONArray jsonObject) {
        this();
        insert(jsonObject);
    }

    Tracker getTracker(String base) {
        for (Tracker tracker : trackers) {
            if (tracker.getBase().equals(base))
                return tracker;
        }
        return null;
    }

    boolean setTracker(String base, Tracker newTracker) {
        for (Tracker tracker : trackers) {
            if (tracker.getBase().equals(base)) {
                tracker.become(newTracker);
                return true;
            }
        }
        return false;
    }

    void addTracker(Tracker newTracker) {
        for (Tracker tracker : trackers) {
            if (tracker.getBase().equals(newTracker.getBase())) {
                tracker.merge(newTracker);
                return;
            }
        }
        trackers.add(newTracker);
    }

    boolean insert(String base, String[] foreign, Double[] sum, Boolean[] notification, Double[] notificationSum) {
        int fSize = foreign.length, sSize = sum.length, nSize = notification.length, nsSize = notificationSum.length;
        if (inequality(fSize, sSize, nSize, nsSize)) {
            Log(LOG_ERROR, "Inequality in sizes given for insert method.", "Current stack trace: " + stackTraceAsString(Thread.currentThread().getStackTrace()));
            return false;
        }
        Tracker tracker = getTracker(base);
        Tracker cTracker = null;
        if (tracker == null) {
            tracker = new Tracker(base);
            trackers.add(tracker);
        } else
            try {
                cTracker = (Tracker) getTracker(base).clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        if (!tracker.add(foreign, sum, notification, notificationSum)) {
            Log(LOG_ERROR, "Error inserting new data.");
            if (!setTracker(base, cTracker))
                addTracker(cTracker);
            return false;
        }
        version += INC;
        return true;
    }


    boolean insert(JSONArray trackerInformation) {
        return handleJSONArray(trackerInformation, MODE_INSERT);
    }


    boolean delete(String base) {
        Tracker toRemove = getTracker(base);
        if (toRemove == null)
            return false;
        trackers.remove(toRemove);
        version += INC;
        return true;
    }

    boolean delete(String base, String foreign) {
        Tracker toRemove = getTracker(base);
        if (toRemove == null)
            return false;
        toRemove.removeAll(foreign);
        version += INC;
        return true;
    }

    boolean delete(String base, String foreign, Double sum) {
        Tracker toRemove = getTracker(base);
        if (toRemove == null)
            return false;
        toRemove.remove(foreign, sum);
        version += INC;
        return true;
    }

    boolean update(String base, String[] foreign, Double[] sum, Boolean[] notification, Double[] notificationSum) {
        int fSize = foreign.length, sSize = sum.length, nSize = notification.length, nsSize = notificationSum.length;
        if (inequality(fSize, sSize, nSize, nsSize)) {
            Log(LOG_ERROR, "Inequality in sizes given for update method.", "Current stack trace: " + stackTraceAsString(Thread.currentThread().getStackTrace()));
            return false;
        }
        Tracker tracker = getTracker(base);
        if (tracker == null)
            return false;
        boolean updated = false;
        for (int i = 0; i < foreign.length; i++)
            if (tracker.set(foreign[i], sum[i], notification[i], notificationSum[i]))
                updated = true;
        if (updated)
            version += INC;
        return updated;
    }

    boolean update(JSONArray updatedInfo) {
        return handleJSONArray(updatedInfo, MODE_UPDATE);
    }

    double getVersion() {
        return version;
    }

    Set<String> getAllBases() {
        Set<String> allBases = new HashSet<>();
        for (Tracker tracker : trackers)
            allBases.add(tracker.getBase());
        return allBases;
    }

    Set<String> getAllForeign(String base) {
        Set<String> allForeigns = new HashSet<>();
        for (Tracker tracker : trackers)
            allForeigns.addAll(tracker.getForeigns());
        return allForeigns;
    }


    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        JSONArray json = convertToJSON();
        try {
            object.put(VERSION, version);
            object.put(INFORMATION, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    private boolean inequality(int... numbers) {
        if (numbers.length == 0)
            return false;
        int compare = numbers[0];
        for (int i = 1; i < numbers.length; i++)
            if (compare != numbers[i])
                return true;
        return false;
    }

    private JSONArray convertToJSON() {
        JSONArray info = new JSONArray();
        for (Tracker tracker : trackers)
            info.put(tracker.toJSON());
        return info;
    }

    private boolean handleJSONArray(JSONArray trackerInformation, int mode) {
        if (trackerInformation.length() > 0)
            try {
                for (int i = 0; i < trackerInformation.length(); i++) {
                    JSONObject baseObject = trackerInformation.getJSONObject(i);

                    JSONArray notifiesSums = baseObject.getJSONArray(NOTIFICATION_AMOUNT);
                    JSONArray notifies = baseObject.getJSONArray(NOTIFICATION);
                    JSONArray amounts = baseObject.getJSONArray(SUM);
                    JSONArray foreigns = baseObject.getJSONArray(FOREIGN);

                    Double[] notifySums = convertToDoubleArray(notifiesSums);
                    Boolean[] notify = convertToBooleanArray(notifies);
                    Double[] sums = convertToDoubleArray(amounts);
                    String[] foreign = convertToStringArray(foreigns);

                    if (mode == MODE_INSERT) {
                        System.out.println(String.format("Adding tracker: %s -> %s", baseObject.getString(BASE), foreign[0]));
                        if (!insert(baseObject.getString(BASE), foreign, sums, notify, notifySums))
                            return false;
                    } else if (mode == MODE_UPDATE) {
                        Log(LOG_INFO, String.format("Updating tracker: %s -> %s", baseObject.getString(BASE), foreign[0]));
                        if (!update(baseObject.getString(BASE), foreign, sums, notify, notifySums))
                            return false;
                    }
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        return false;
    }

    private Double[] convertToDoubleArray(JSONArray arr) throws JSONException {
        Double[] dArr = new Double[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            dArr[i] = arr.getDouble(i);
        }
        return dArr;
    }

    private Boolean[] convertToBooleanArray(JSONArray arr) throws JSONException {
        Boolean[] bArr = new Boolean[arr.length()];
        for (int i = 0; i < arr.length(); i++)
            bArr[i] = arr.getBoolean(i);
        return bArr;
    }

    private String[] convertToStringArray(JSONArray arr) throws JSONException {
        String[] sArr = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++)
            sArr[i] = arr.getString(i);
        return sArr;
    }
}

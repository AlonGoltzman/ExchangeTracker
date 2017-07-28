package com.alon.exchangetrackerserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.*;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.Log;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.loadKey;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.stackTraceAsString;

/**
 * Created by Alon on 6/28/2017.
 */
public class Tracker implements Cloneable {

    private String base;
    private ArrayList<String> foreigns;
    private ArrayList<Double> sums;
    private ArrayList<Boolean> notify;
    private ArrayList<Double> notifySums;

    Tracker(String b) {
        base = b;
        foreigns = new ArrayList<>();
        sums = new ArrayList<>();
        notify = new ArrayList<>();
        notifySums = new ArrayList<>();
    }

    boolean add(String[] fArr, Double[] sArr, Boolean[] nArr, Double[] nSArr) {
        Objects.requireNonNull(fArr);
        Objects.requireNonNull(sArr);
        Objects.requireNonNull(nArr);
        Objects.requireNonNull(nSArr);
        if (inequality(fArr.length, sArr.length, nArr.length, nSArr.length)) {
            Log(LOG_ERROR, "Inequality in sizes given for insert method.", "Current stack trace: " + stackTraceAsString(Thread.currentThread().getStackTrace()));
            return false;
        }
        for (int i = 0; i < fArr.length; i++) {
            foreigns.add(fArr[i]);
            sums.add(sArr[i]);
            notify.add(nArr[i]);
            notifySums.add(nSArr[i]);
        }
        return true;
    }


    void become(Tracker to) {
        Objects.requireNonNull(to);
        base = to.getBase();
        foreigns.clear();
        sums.clear();
        notify.clear();
        notifySums.clear();
        foreigns.addAll(to.getForeigns());
        sums.addAll(to.getSums());
        notify.addAll(to.getNotify());
        notifySums.addAll(to.getNotifySums());
    }

    void merge(Tracker with) {
        Objects.requireNonNull(with);
        if (with.getBase() == null || with.getBase().isEmpty())
            throw new IllegalArgumentException();
        foreigns.addAll(with.getForeigns());
        sums.addAll(with.getSums());
        notify.addAll(with.getNotify());
        notifySums.addAll(with.getNotifySums());
    }

    void removeAll(String foreign) {
        for (int i = 0; i < foreigns.size(); i++)
            if (foreigns.get(i).equals(foreign)) {
                removeEntry(i);
                return;
            }
    }

    void remove(String foreign, Double sum) {
        for (int i = 0; i < foreigns.size(); i++)
            if (foreigns.get(i).equals(foreign) && sums.get(i).equals(sum)) {
                removeEntry(i);
                return;
            }
    }


    boolean set(String foreign, Double sum, Boolean notification, Double notificationSum) {
        int pos = foreigns.indexOf(foreign);
        if (pos < 0)
            return false;
        sums.set(pos, sum);
        notify.set(pos, notification);
        notifySums.set(pos, notificationSum);
        return true;
    }

    JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(BASE, base);

            JSONArray fArr = new JSONArray();
            JSONArray sArr = new JSONArray();
            JSONArray nArr = new JSONArray();
            JSONArray nSArr = new JSONArray();

            for (int i = 0; i < foreigns.size(); i++) {
                fArr.put(foreigns.get(i));
                sArr.put(sums.get(i));
                nArr.put(notify.get(i));
                nSArr.put(notifySums.get(i));
            }

            obj.put(FOREIGN, fArr);
            obj.put(SUM, sArr);
            obj.put(NOTIFICATION, nArr);
            obj.put(NOTIFICATION_AMOUNT, nSArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }


    String getBase() {
        return base;
    }

    List<String> getForeigns() {
        return Collections.unmodifiableList(foreigns);
    }

    List<Double> getSums() {
        return Collections.unmodifiableList(sums);
    }

    List<Boolean> getNotify() {
        return Collections.unmodifiableList(notify);
    }

    List<Double> getNotifySums() {
        return Collections.unmodifiableList(notifySums);
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


    private void removeEntry(int i) {
        foreigns.remove(i);
        sums.remove(i);
        notify.remove(i);
        notifySums.remove(i);
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        super.clone();
        Tracker tracker = new Tracker(getBase());
        String[] f = new String[foreigns.size()];
        Double[] s = new Double[sums.size()];
        Boolean[] n = new Boolean[notify.size()];
        Double[] nS = new Double[notifySums.size()];
        tracker.add(f, s, n, nS);
        return tracker;
    }


}

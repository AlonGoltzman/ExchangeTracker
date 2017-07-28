package com.alon.exchangetracker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTIVITY_HANDLER_RELOAD_DATASET;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.BASE;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.FOREIGN;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.INFORMATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION_AMOUNT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SUM;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.VERSION;

/**
 * Created by Alon on 6/24/2017.
 */

final class Trackers implements Serializable, Iterable<Tracker> {

    static final String TRACKERS_FILE_NAME = "trackers.xchange";
    private static Trackers instance;

    private volatile ArrayList<Tracker> trackers;
    private double version = 0;

    private TrackerUpdateListener listener;
    private Handler mainThreadHandler;

    static Trackers getInstance() {
        return instance == null ? new Trackers() : instance;
    }

    private Trackers() {
        instance = this;
        trackers = new ArrayList<>();
    }

    void addTracker(Tracker tracker) {
        trackers.add(tracker);
        updateAdapter();
    }

    Tracker getTracker(int index) {
        return trackers.get(index);
    }

    int size() {
        return trackers.size();
    }

    Set<String> allBaseCurrencies() {
        Set<String> set = new HashSet<>();
        for (Tracker tracker : trackers)
            set.add(tracker.getBase());
        return set;
    }

    Set<String> allForeignCurrenciesForBase(String base) {
        Set<String> set = new HashSet<>();
        for (Tracker tracker : trackers)
            if (tracker.getBase().equals(base))
                set.add(tracker.getForeign());
        return set;
    }

    HashMap<String, Double> allSumsForBase(String base) {
        HashMap<String, Double> map = new HashMap<>();
        for (Tracker tracker : trackers)
            if (tracker.getBase().equals(base))
                map.put(tracker.getForeign(), tracker.getSum());
        return map;
    }

    ArrayList<Boolean> shouldNotifyForBases(String base) {
        ArrayList<Boolean> list = new ArrayList<>();
        for (Tracker tracker : trackers) {
            if (tracker.getBase().equals(base))
                list.add(tracker.getNotify());
        }
        return list;
    }

    ArrayList<Double> notificationSums(String base) {
        ArrayList<Double> list = new ArrayList<>();
        for (Tracker tracker : trackers)
            if (tracker.getBase().equals(base))
                list.add(tracker.getNotifySum());
        return list;
    }

    TrackerUpdateListener getListener() {
        return listener;
    }

    void update(int position, Tracker newTracker) {
        if (position > trackers.size() - 1)
            throw new IndexOutOfBoundsException();
        if (trackers.get(position).equals(newTracker))
            return;
        trackers.set(position, newTracker);
        updateAdapter();
    }


    private void removeTracker(int position) {
        trackers.remove(position);
        updateAdapter();
    }

    static void remove(int position) {
        instance.removeTracker(position);
    }

    boolean notificationRequired(String base, String foreign, Double rate) {
        for (Tracker tracker : trackers) {
            if (tracker.getBase().equals(base) && tracker.getForeign().equals(foreign))
                return tracker.getSum() * rate >= tracker.getNotifySum();
        }
        return false;
    }


    void setVersion(double version) {
        if (version > this.version)
            this.version = version;
    }

    double getVersion() {
        return version;
    }

    boolean contains(Tracker addedTracker) {
        return trackers.contains(addedTracker);
    }

    synchronized void save(File file) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
        stream.writeObject(getSerializable());
        stream.flush();
        stream.close();
    }

    static Trackers load(File file) {
        try {
            if (!file.exists())
                return getInstance();
            instance = merge((SerializableTrackers) new ObjectInputStream(new FileInputStream(file)).readObject());
        } catch (ClassCastException e) {
            Log.e("Trackers", "load: Invalid file.", e);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return instance;
    }

    void clear() {
        trackers.clear();
        updateAdapter();
    }


    private void updateAdapter() {
        version += 0.1;
        if (listener != null) {
            boolean result = listener.updated();
            if (!result) {
                if (mainThreadHandler != null) {
                    Message updateMessage = new Message();
                    updateMessage.what = ACTIVITY_HANDLER_RELOAD_DATASET;
                    mainThreadHandler.sendMessage(updateMessage);
                }
            }
        }
    }

    private Serializable getSerializable() {
        return new SerializableTrackers(this);
    }

    private static Trackers merge(SerializableTrackers trackers) {
        if (instance == null)
            getInstance();
        instance.trackers = trackers.getTrackers();
        instance.setVersion(trackers.getVersion());
        return instance;
    }


    JSONObject toJSON() {
        try {
            JSONObject root = new JSONObject();
            JSONArray arr = new JSONArray();
            ArrayList<JSONObject> currencies = new ArrayList<>();
            for (Tracker tracker : trackers) {
                boolean found = false;
                for (JSONObject currency : currencies) {
                    if (currency.getString(BASE) != null)
                        if (currency.getString(BASE).equals(tracker.getBase())) {
                            JSONArray foreign = currency.getJSONArray(FOREIGN);
                            JSONArray sums = currency.getJSONArray(SUM);
                            JSONArray notify = currency.getJSONArray(NOTIFICATION);
                            JSONArray notifySums = currency.getJSONArray(NOTIFICATION_AMOUNT);

                            foreign.put(tracker.getForeign());
                            sums.put(tracker.getSum());
                            notify.put(tracker.getNotify());
                            notifySums.put(tracker.getNotifySum());

                            currency.put(FOREIGN, foreign).put(SUM, sums).put(NOTIFICATION, notify).put(NOTIFICATION_AMOUNT, notifySums);
                            found = true;
                            break;
                        }
                }
                if (!found) {
                    JSONObject newCurrency = new JSONObject();
                    newCurrency.put(BASE, tracker.getBase());

                    JSONArray foreign = new JSONArray();
                    JSONArray sums = new JSONArray();
                    JSONArray notify = new JSONArray();
                    JSONArray notifySums = new JSONArray();

                    foreign.put(tracker.getForeign());
                    sums.put(tracker.getSum());
                    notify.put(tracker.getNotify());
                    notifySums.put(tracker.getNotifySum());

                    newCurrency.put(FOREIGN, foreign).put(SUM, sums).put(NOTIFICATION, notify).put(NOTIFICATION_AMOUNT, notifySums);

                    currencies.add(newCurrency);
                }
            }

            for (JSONObject currency : currencies)
                arr.put(currency);
            root.put(VERSION, version).put(INFORMATION, arr);

            return root;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Iterator<Tracker> iterator() {
        return trackers.iterator();
    }


    ArrayList<Tracker> getTrackers() {
        return trackers;
    }

    void setTracker(Tracker creationTracker, Tracker newTracker) {
        for (int i = 0; i < trackers.size(); i++)
            if (creationTracker.equals(trackers.get(i))) {
                trackers.set(i, newTracker);
                updateAdapter();
                return;
            }
    }

    void setListener(TrackerUpdateListener listener) {
        this.listener = listener;
    }

    void setMainThreadHandler(Handler mainThreadHandler) {
        this.mainThreadHandler = mainThreadHandler;
    }
}

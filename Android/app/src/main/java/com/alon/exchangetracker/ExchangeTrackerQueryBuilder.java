package com.alon.exchangetracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;

import static com.alon.exchangetracker.ExchangeTrackerUtility.encrypt;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.BASE;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.FOREIGN;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.INFORMATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION_AMOUNT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SUM;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.VERSION;

final class ExchangeTrackerQueryBuilder {

    private final int STATE_CREATED = 0;
    private final int STATE_STARTED = 1;
    private final int STATE_ACTION_SET = 2;
    private final int STATE_UID_SET = 3;
    private final int STATE_VERSION_SET = 4;
    private final int STATE_INFORMATION_POUR_INITIATED = 5;
    private final int STATE_TRACKER_BASE_SET = 6;
    private final int STATE_TRACKER_FOREIGN_SET = 7;
    private final int STATE_TRACKER_SUMS_SET = 8;
    private final int STATE_TRACKER_NOTIFICATIONS_SET = 9;
    private final int STATE_TRACKER_NOTIFICATION_SUMS_SET = 10;
    private final int STATE_INFORMATION_POUR_FINISHED = 11;
    private final int STATE_BEGUN_RATE_PULL_CONFIG = 12;
    private final int STATE_FINISHED_RATE_PULL_CONFIG = 13;


    private JSONObject query;
    private JSONArray information;
    private JSONObject tracker;
    private int state = STATE_CREATED;
    private int index = -1;

    ExchangeTrackerQueryBuilder() {
        state = STATE_CREATED;
    }

    ExchangeTrackerQueryBuilder begin() {
        ensureState(STATE_CREATED);
        query = new JSONObject();
        state = STATE_STARTED;
        return this;
    }

    ExchangeTrackerQueryBuilder action(String action) throws JSONException {
        ensureState(STATE_STARTED);
        query.put(ACTION, action);
        state = STATE_ACTION_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder uid(String uid) throws JSONException {
        ensureState(STATE_ACTION_SET);
        query.put(UID, uid);
        state = STATE_UID_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder version(Double version) throws JSONException {
        ensureState(STATE_UID_SET);
        query.put(VERSION, version);
        state = STATE_VERSION_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder base(String base) throws JSONException {
        ensureState(STATE_VERSION_SET);
        query.put(BASE, base);
        state = STATE_BEGUN_RATE_PULL_CONFIG;
        return this;
    }

    ExchangeTrackerQueryBuilder foreign(String foreign) throws JSONException {
        ensureState(STATE_BEGUN_RATE_PULL_CONFIG);
        query.put(FOREIGN, foreign);
        state = STATE_FINISHED_RATE_PULL_CONFIG;
        return this;
    }

    ExchangeTrackerQueryBuilder initInformationPour() {
        ensureState(STATE_VERSION_SET);
        state = STATE_INFORMATION_POUR_INITIATED;
        information = new JSONArray();
        return this;
    }

    ExchangeTrackerQueryBuilder openNewTracker(String base) throws JSONException {
        ensureState(STATE_INFORMATION_POUR_INITIATED);
        tracker = new JSONObject();
        tracker.put(BASE, base);
        state = STATE_TRACKER_BASE_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder setForeignCurrencies(String... foreignCurrencies) throws JSONException {
        ensureState(STATE_TRACKER_BASE_SET);
        tracker.put(FOREIGN, convertToJSONArray(foreignCurrencies));
        state = STATE_TRACKER_FOREIGN_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder setSums(Double... sums) throws JSONException {
        ensureState(STATE_TRACKER_FOREIGN_SET);
        tracker.put(SUM, convertToJSONArray(sums));
        state = STATE_TRACKER_SUMS_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder setNotifications(Boolean... notifications) throws JSONException {
        ensureState(STATE_TRACKER_SUMS_SET);
        tracker.put(NOTIFICATION, convertToJSONArray(notifications));
        state = STATE_TRACKER_NOTIFICATIONS_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder setNotificationSums(Double... notificationSums) throws JSONException {
        ensureState(STATE_TRACKER_NOTIFICATIONS_SET);
        tracker.put(NOTIFICATION_AMOUNT, convertToJSONArray(notificationSums));
        state = STATE_TRACKER_NOTIFICATION_SUMS_SET;
        return this;
    }

    ExchangeTrackerQueryBuilder closeTracker() throws JSONException {
        ensureState(STATE_TRACKER_NOTIFICATION_SUMS_SET, STATE_TRACKER_FOREIGN_SET);
        JSONObject object = new JSONObject(tracker.toString());
        information.put(object);
        tracker = null;
        state = STATE_INFORMATION_POUR_INITIATED;
        return this;
    }

    ExchangeTrackerQueryBuilder finishInformationPour() throws JSONException {
        ensureState(STATE_INFORMATION_POUR_INITIATED);
        query.put(INFORMATION, information);
        state = STATE_INFORMATION_POUR_FINISHED;
        return this;
    }

    JSONObject finishQueryGeneration() throws JSONException {
        ensureState(STATE_INFORMATION_POUR_FINISHED, STATE_VERSION_SET, STATE_FINISHED_RATE_PULL_CONFIG);
        JSONObject object = new JSONObject(query.toString());
        query = null;
        return object;
    }

    String finishQueryGenerationWithEncryption(Key key) throws JSONException {
        ensureState(STATE_INFORMATION_POUR_FINISHED, STATE_VERSION_SET, STATE_FINISHED_RATE_PULL_CONFIG);
        JSONObject object = new JSONObject(query.toString());
        query = null;
        System.out.println("closed: " + index);
        return encrypt(object.toString(), key);
    }

    private void ensureState(int... expectedState) {
        boolean isState = false;
        for (int s : expectedState)
            if (state == s) {
                isState = true;
                break;
            }
        if (!isState)
            throw new IllegalStateException("Didn't create Query properly.");
    }

    private <T> JSONArray convertToJSONArray(T[] objects) {
        JSONArray arr = new JSONArray();
        for (T object : objects)
            arr.put(object);
        return arr;
    }
}

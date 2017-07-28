package com.alon.exchangetrackerserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.*;
import static com.alon.exchangetrackerserver.ExchangeTrackerTrackers.*;
import static com.alon.exchangetrackerserver.ExchangeTrackerUIDList.UIDExists;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.*;

public final class ExchangeTracker extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureOS(request.getHeader("User-Agent"))) {
            response.sendError(400);
            return;
        }
        InputStream inStream = request.getInputStream();
        byte[] buffer = new byte[64];
        int aRead;
        StringBuilder jsonBuilder = new StringBuilder();

        while ((aRead = inStream.read(buffer)) != -1)
            jsonBuilder.append(new String(buffer, 0, aRead));

        inStream.close();
        String actualJSON = decrypt(jsonBuilder.toString(), loadKey(getServletContext()));

        assert actualJSON != null;
        JSONObject object;
        try {
            object = new JSONObject(actualJSON);
        } catch (JSONException e) {
            response.getWriter().write(INVALID_MESSAGE);
            return;
        }
        String uid;
        try {
            uid = object.getString(UID);
        } catch (JSONException e1) {
            response.getWriter().write(ERR_NO_UID);
            return;
        }
        String action;
        try {
            action = object.getString(ACTION);
        } catch (JSONException e) {
            response.getWriter().write(ERR_NO_ACTION);
            return;
        }
        if (action.equals(ACTION_VERIFY_UID)) {
            response.getWriter().write(Boolean.toString(UIDExists(uid)));
            return;
        }
        if (!UIDExists(uid)) {
            response.getWriter().write(ERR_UID_DOESNT_EXIST);
            return;
        }


        execute(response, object, uid, action);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    private void execute(HttpServletResponse response, JSONObject object, String uid, String action) throws IOException {
        switch (action) {
            case ACTION_DELETE_ALL_TRACKERS:
                Log(LOG_INFO, "Deleting all trackers.");
                deleteAllTrackers(uid);
                break;
            case ACTION_DELETE_BASE:
                try {
                    String baseToDelete = object.getString(BASE);
                    Log(LOG_INFO, "Deleting tracker " + baseToDelete);
                    deleteTracker(uid, baseToDelete);
                    break;
                } catch (JSONException e) {
                    response.getWriter().write(ERR_NO_BASE);
                    return;
                }
            case ACTION_DELETE_CERTAIN_TRACKERS:
                Log(LOG_INFO, "Deleting certain tracker.");
                try {
                    JSONArray infoArr = object.getJSONArray(INFORMATION);
                    for (int i = 0; i < infoArr.length(); i++) {
                        JSONObject trackerInfo = infoArr.getJSONObject(i);
                        if (!deleteTrackerForForeignCurrency(uid, trackerInfo.getString(BASE), trackerInfo.getJSONArray(FOREIGN).getString(0))) {
                            response.getWriter().write(ERR_NOT_TRACKER_FOUND);
                            return;
                        }
                        System.out.println(String.format("Removed tracker: %s -> %s", trackerInfo.getString(BASE), trackerInfo.getJSONArray(FOREIGN).getString(0)));
                    }
                    break;
                } catch (JSONException e) {
                    response.getWriter().write(ERR_NO_INFORMATION);
                    return;
                }
            case ACTION_INSERT_TRACKERS:
            case ACTION_UPDATE_TRACKERS:
                try {
                    JSONArray infoArr = object.getJSONArray(INFORMATION);
                    if (action.equals(ACTION_INSERT_TRACKERS) ? !insertNewTracker(uid, infoArr) : !updateTracker(uid, infoArr)) {
                        response.getWriter().write(ERR_INSERTION_FAILED);
                        return;
                    }
                    ExchangeTrackerRatesThread.getInstance().needsUpdate();
                    break;
                } catch (JSONException e) {
                    response.getWriter().write(ERR_NO_INFORMATION);
                    return;
                }
            case ACTION_SHOULD_UPDATE_CLIENT:
                try {
                    Double version = object.getDouble(VERSION);
                    System.out.println("Needs to update: " + Boolean.toString(shouldPullTracker(uid, version)));
                    response.getWriter().write(Boolean.toString(shouldPullTracker(uid, version)));
                    return;
                } catch (JSONException e) {
                    response.getWriter().write(ERR_NO_VERSION);
                    return;
                }
            case ACTION_PULL_TRACKERS:
                if (getTracker(uid) != null) {
                    String json = getTracker(uid).toString();
                    System.out.println("Trackers being sent out:" + json);
                    String encrypted = encrypt(json, loadKey(getServletContext()));
                    if (encrypted == null) {
                        response.sendError(500);
                        return;
                    }
                    response.getOutputStream().write(encrypted.getBytes());
                } else
                    response.getWriter().write(OK);
                return;
            case ACTION_PULL_RATES:
                try {
                    Log(LOG_INFO, "Pulling rates.");
                    String base = object.getString(BASE);
                    String foreign = object.getString(FOREIGN);
                    response.getWriter().write(ExchangeTrackerRates.rates(base, foreign));
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                    response.sendError(500);
                    return;
                }
        }
        response.getWriter().write(OK);
    }

}

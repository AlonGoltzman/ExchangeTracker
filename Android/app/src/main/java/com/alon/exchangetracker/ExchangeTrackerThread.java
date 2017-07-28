package com.alon.exchangetracker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alon.exchangetracker.commons.ExchangeTrackerConstants;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import static com.alon.exchangetracker.ExchangeTrackerUtility.decrypt;
import static com.alon.exchangetracker.ExchangeTrackerUtility.encrypt;
import static com.alon.exchangetracker.ExchangeTrackerUtility.openConnection;
import static com.alon.exchangetracker.ExchangeTrackerUtility.processConnection;
import static com.alon.exchangetracker.ExchangeTrackerUtility.reloadTrackers;
import static com.alon.exchangetracker.Trackers.TRACKERS_FILE_NAME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_DELETE_ALL_TRACKERS;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_INSERT_TRACKERS;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_PULL_RATES;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_PULL_TRACKERS;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_VERIFY_UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.BASE;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.FOREIGN;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.INFORMATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.NOTIFICATION_AMOUNT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.OK;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_DOWNLOAD_STATE_CHANGED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_MIM;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_NOTIFY;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_SAVE_KEY_AND_UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SUM;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.VERSION;

class ExchangeTrackerThread extends Thread {

    static final String BASE_URL = "http://10.0.2.2:8080/ExchangeTracker";
    final String TAG = "TrackerThread";

    private static Handler mainThreadHandler;

    private boolean work = true;
    private boolean incorrectHash = false;
    private boolean isApplicationAlive = true;
    private long nextDownloadIteration = -1;

    private boolean shouldPullUIDAndKey = false;
    private boolean verifyUID = true;

    private boolean requestedUpload = false;
    private boolean requestedDownload = false;
    private boolean requestedRates = false;
    private boolean requestedSync = false;
    private boolean requestedUID = false;

    private PublicKey pKey;
    private String UID;
    private String hash;
    private File parentDir;
    private Trackers trackers;
    private File trackersFile;

    ExchangeTrackerThread(File directory, String uid) {
        parentDir = directory;
        trackersFile = new File(parentDir, TRACKERS_FILE_NAME);
        trackers = Trackers.getInstance();
        nextDownloadIteration = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        try {
            pKey = (PublicKey) new ObjectInputStream(new FileInputStream(new File(parentDir, "pKey"))).readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (uid != null)
            UID = uid;
    }

    ExchangeTrackerThread(File directory) {
        this(directory, null);
    }

    @Override
    public void run() {
        try {
            while (work) {
                if (shouldPullUIDAndKey) {
                    getKey();
                    ensureCorrectHash();
                    getUID();
                    sendMessage(incorrectHash ? SERVICE_HANDLER_MIM : SERVICE_HANDLER_SAVE_KEY_AND_UID, incorrectHash ? null : UID);
                    shouldPullUIDAndKey = false;
                } else if (verifyUID)
                    if (UID != null)
                        verifyUID = false;
                    else {
                        JSONObject object = new JSONObject();
                        object.put(ACTION, ACTION_VERIFY_UID).put(ExchangeTrackerConstants.UID, UID);
                        String request = encrypt(object.toString(), pKey);
                        boolean verified = Boolean.parseBoolean(processConnection(openConnection(BASE_URL), request));
                        if (!verified)
                            requestedUID = true;
                        verifyUID = false;
                    }
                if (requestedUID) {
                    getUID();
                    sendMessage(SERVICE_HANDLER_SAVE_KEY_AND_UID, UID);
                    requestedUID = false;
                }
                if (requestedUpload) {
                    uploadTrackers();
                    requestedUpload = false;
                }
                if (requestedDownload) {
                    boolean downloaded = downloadTrackers();
                    sendMessage(SERVICE_HANDLER_DOWNLOAD_STATE_CHANGED, downloaded);
                    requestedDownload = false;
                }
                if (requestedRates) {
                    if (hasTrackers())
                        processRates();
                    requestedRates = false;
                }
                if (requestedSync || nextDownloadIteration < System.currentTimeMillis()) {
                    requestedUpload = requestedRates = true;
                    if (!requestedSync)
                        nextDownloadIteration = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
                    else
                        requestedSync = false;
                }
            }
        } catch (IOException | NoSuchAlgorithmException | ClassNotFoundException | JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean downloadTrackers() throws JSONException {
        String encrypted = new ExchangeTrackerQueryBuilder().begin().action(ACTION_PULL_TRACKERS).uid(UID).version(trackers.getVersion()).finishQueryGenerationWithEncryption(pKey);
        String response = processConnection(openConnection(BASE_URL), encrypted);
        response = decrypt(response, pKey);
        if (response != null && !response.isEmpty() && !response.equals(OK)) {
            JSONObject object = new JSONObject(response);
            trackers.setVersion(Double.parseDouble(object.get(VERSION).toString()));
            trackers.clear();
            JSONArray info = object.getJSONArray(INFORMATION);
            for (int i = 0; i < info.length(); i++) {
                JSONObject tracker = info.getJSONObject(i);
                String base = tracker.getString(BASE);
                JSONArray foreign = tracker.getJSONArray(FOREIGN);
                JSONArray sums = tracker.getJSONArray(SUM);
                JSONArray notify = tracker.getJSONArray(NOTIFICATION);
                JSONArray notifySums = tracker.getJSONArray(NOTIFICATION_AMOUNT);

                for (int j = 0; j < foreign.length(); j++)
                    trackers.addTracker(
                            new Tracker(base,
                                    foreign.getString(i),
                                    Double.parseDouble(sums.get(i).toString()),
                                    notify.getBoolean(i),
                                    Double.parseDouble(notifySums.get(i).toString())));
            }
            return true;
        } else if (response != null && (response.isEmpty() || response.equals(OK)))
            return true;
        return false;
    }

    private void uploadTrackers() throws JSONException {
        JSONObject trackersJSON = trackers.toJSON();
        if (trackersJSON == null || !hasTrackers()) {
            String encrypted = new ExchangeTrackerQueryBuilder().begin().action(ACTION_DELETE_ALL_TRACKERS).uid(UID).version(trackers.getVersion()).finishQueryGenerationWithEncryption(pKey);
            processConnection(openConnection(BASE_URL), encrypted);
            return;
        }
        trackersJSON.put(ACTION, ACTION_INSERT_TRACKERS).put(ExchangeTrackerConstants.UID, UID);

        if (pKey != null) {
            String encrypted = encrypt(trackersJSON.toString(), pKey);
            String result = processConnection(openConnection(BASE_URL), encrypted);
            if (!result.equals(OK)) {
                Log.w(TAG, "requestedUpload: Data upload returned:" + result);
            }
        } else {
            Log.w(TAG, "requestedUpload: Failed uploading trackers, no key.");
        }
    }

    private boolean hasTrackers() {
        if (!isApplicationAlive)
            trackers = reloadTrackers(trackers, trackersFile);
        return trackers.size() > 0;
    }

    private void processRates() {
        trackers = Trackers.load(trackersFile);
        for (String base : trackers.allBaseCurrencies())
            for (String foreign : trackers.allForeignCurrenciesForBase(base))
                try {
                    String encryptedJSON = new ExchangeTrackerQueryBuilder()
                            .begin()
                            .action(ACTION_PULL_RATES)
                            .uid(UID)
                            .version(-1D)
                            .base(base).foreign(foreign)
                            .finishQueryGenerationWithEncryption(pKey);
                    String response = processConnection(openConnection(BASE_URL), encryptedJSON);
                    if (response != null) {
                        Double rate = Double.parseDouble(response);
                        if (trackers.notificationRequired(base, foreign, rate))
                            sendMessage(SERVICE_HANDLER_NOTIFY);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
    }


    private void sendMessage(int what, Object obj) {
        if (mainThreadHandler != null) {
            Message msg = new Message();
            msg.what = what;
            if (obj != null)
                msg.obj = obj;
            mainThreadHandler.sendMessage(msg);
        }
    }

    private void sendMessage(int what) {
        sendMessage(what, null);
    }

    private void getKey() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        HttpURLConnection conn = openConnection(BASE_URL + "/key");
        conn.connect();

        InputStream inStream = conn.getInputStream();

        byte[] keyInput = IOUtils.toByteArray(inStream);

        inStream.close();

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] response = md.digest(keyInput);
        StringBuilder sb = new StringBuilder();
        for (byte aResponse : response)
            sb.append(Integer.toString((aResponse & 0xff) + 0x100, 16).substring(1));

        hash = sb.toString();

        FileOutputStream fileStream = new FileOutputStream(new File(parentDir, "pKey"));
        fileStream.write(keyInput);
        fileStream.close();

        ObjectInputStream objStream = new ObjectInputStream(new FileInputStream(new File(parentDir, "pKey")));
        pKey = (PublicKey) objStream.readObject();
    }

    private void ensureCorrectHash() throws IOException {
        incorrectHash = !(processConnection(openConnection(BASE_URL + "/hash")).equals(hash));
    }

    private void getUID() throws IOException {
        UID = decrypt(processConnection(openConnection(BASE_URL + "/uid")), pKey);
        trackers = reloadTrackers(trackers, trackersFile);
    }


    void canWork(boolean flag) {
        work = flag;
    }

    void shouldPullInfo(boolean flag) {
        shouldPullUIDAndKey = flag;
    }

    void setApplicationIsAlive(boolean flag) {
        isApplicationAlive = flag;
    }

    void setHandler(Handler handler) {
        mainThreadHandler = handler;
    }

    void setShouldVerifyUID(boolean flag) {
        verifyUID = flag;
    }

    void setRequestedSync(boolean flag) {
        requestedSync = flag;
    }

    void setRequestedNewUID(boolean uidRequested) {
        this.requestedUID = uidRequested;
    }

    void setRequestedUpload(boolean requestedUpload) {
        this.requestedUpload = requestedUpload;
    }

    void setRequestedDownload(boolean requestedDownload) {
        this.requestedDownload = requestedDownload;
    }

    void setRequestedRates(boolean requestedRates) {
        this.requestedRates = requestedRates;
    }


    private Object getFromJSONObject(JSONObject obj, String name) {
        try {
            return obj.get(name);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


}

package com.alon.exchangetracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_ACTIVITY_KILLED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_INIT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_KILL;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_PAUSE;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_DOWNLOAD;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_KEY;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_RATES;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_SYNC;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_UPLOAD;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_RESUME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_BROADCAST_DOWNLOAD_FAILED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_BROADCAST_DOWNLOAD_WORKED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_DOWNLOAD_STATE_CHANGED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_MIM;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_NOTIFY;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_HANDLER_SAVE_KEY_AND_UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_NAME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_UID_KILL;

public class ExchangeTrackerService extends Service {

    private ExchangeTrackerThread thread;
    private static CustomHandler mainThreadHandler;

    public ExchangeTrackerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (mainThreadHandler == null)
            mainThreadHandler = new CustomHandler();
        if (!mainThreadHandler.hasSharedPrefs())
            mainThreadHandler.setSharedPrefs(getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_APPEND));
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case SERVICE_ACTION_INIT:
                    initThread();
                    thread.shouldPullInfo(true);
                    thread.setHandler(mainThreadHandler);
                    thread.start();
                    break;
                case SERVICE_ACTION_RESUME:
                    initThread();
                    thread.canWork(true);
                    thread.shouldPullInfo(false);
                    thread.setShouldVerifyUID(true);
                    thread.start();
                    break;
                case SERVICE_ACTION_PAUSE:
                    thread.canWork(false);
                    break;
                case SERVICE_ACTION_KILL:
                    thread.canWork(false);
                    thread.interrupt();
                    thread = null;
                    break;
                case SERVICE_ACTION_ACTIVITY_KILLED:
                    thread.setApplicationIsAlive(false);
                    break;
                case SERVICE_ACTION_REQUEST_SYNC:
                    thread.setRequestedSync(true);
                    break;
                case SERVICE_ACTION_REQUEST_KEY:
                    thread.setRequestedNewUID(true);
                    break;
                case SERVICE_ACTION_REQUEST_RATES:
                    thread.setRequestedRates(true);
                    break;
                case SERVICE_ACTION_REQUEST_DOWNLOAD:
                    thread.setRequestedDownload(true);
                    break;
                case SERVICE_ACTION_REQUEST_UPLOAD:
                    thread.setRequestedUpload(true);
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initThread() {
        if (thread != null) {
            if (!thread.getState().equals(Thread.State.NEW))
                thread = new ExchangeTrackerThread(getFilesDir(), ExchangeTrackerUtility.getInstance().getUID(this));
        } else
            thread = new ExchangeTrackerThread(getFilesDir(), ExchangeTrackerUtility.getInstance().getUID(this));
    }


    @SuppressLint("HandlerLeak")
    private class CustomHandler extends Handler {

        private SharedPreferences sharedPrefs;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_HANDLER_SAVE_KEY_AND_UID:
                    String uid = (String) msg.obj;
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(SHARED_PREFERENCES_UID, uid);
                    editor.putLong(SHARED_PREFERENCES_UID_KILL, getTerminationDate());
                    editor.apply();
                    break;
                case SERVICE_HANDLER_MIM:
                    Toast.makeText(ExchangeTrackerService.this, getString(R.string.MIM), Toast.LENGTH_SHORT).show();
                    thread.canWork(false);
                    break;
                case SERVICE_HANDLER_NOTIFY:
                    NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                    Notification notif = new NotificationCompat.Builder(ExchangeTrackerService.this).setContentTitle("Exchange Alert!").setContentText("One of your trackers reported to have passed the minimum you set.").setSmallIcon(R.mipmap.ic_launcher).build();
                    manager.notify(100, notif);
                    break;
                case SERVICE_HANDLER_DOWNLOAD_STATE_CHANGED:
                    boolean worked = (boolean) msg.obj;
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(worked ? SERVICE_BROADCAST_DOWNLOAD_WORKED : SERVICE_BROADCAST_DOWNLOAD_FAILED));
                    break;
            }
        }

        private long getTerminationDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.MONTH, 1);
            Date date = cal.getTime();
            return date.getTime();
        }

        void setSharedPrefs(SharedPreferences sharedPrefs) {
            this.sharedPrefs = sharedPrefs;
        }

        boolean hasSharedPrefs() {
            return sharedPrefs != null;
        }

    }
}

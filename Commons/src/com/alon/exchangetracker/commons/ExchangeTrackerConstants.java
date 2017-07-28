package com.alon.exchangetracker.commons;

public final class ExchangeTrackerConstants {

    public static final String ACTION = "action";
    public static final String UID = "UID";
    public static final String BASE = "baseCurrency";
    public static final String FOREIGN = "foreignCurrencies";
    public static final String SUM = "currencySum";
    public static final String NOTIFICATION = "shouldNotify";
    public static final String NOTIFICATION_AMOUNT = "notificationAmount";
    public static final String INFORMATION = "information";
    public static final String VERSION = "version";

    public static final int LOG_ERROR = 0;
    public static final int LOG_INFO = 1;

    public static final int MODE_UPDATE = 10;
    public static final int MODE_INSERT = 11;

    public static final String OK = "0";
    public static final String INVALID_MESSAGE = "-1";
    public static final String ERR_NO_UID = "-2";
    public static final String ERR_UID_DOESNT_EXIST = "-3";
    public static final String ERR_NO_ACTION = "-4";
    public static final String ERR_NO_BASE = "-5";
    public static final String ERR_NO_INFORMATION = "-6";
    public static final String ERR_NOT_TRACKER_FOUND = "-7";
    public static final String ERR_INSERTION_FAILED = "-8";
    public static final String ERR_NO_VERSION = "-9";

    public static final String ACTION_INSERT_TRACKERS = "newTrackers";
    public static final String ACTION_UPDATE_TRACKERS = "updateTrackers";
    public static final String ACTION_DELETE_CERTAIN_TRACKERS = "deleteForeignCurrencies";
    public static final String ACTION_DELETE_ALL_TRACKERS = "deleteAll";
    public static final String ACTION_DELETE_BASE = "deleteBaseCurrency";
    public static final String ACTION_SHOULD_UPDATE_CLIENT = "needsTrackerUpdate";
    public static final String ACTION_PULL_TRACKERS = "pullTrackers";
    public static final String ACTION_PULL_RATES = "pullRates";
    public static final String ACTION_VERIFY_UID = "verifyUID";

    public static final String SERVICE_ACTION_INIT = "startWorkerThread";
    public static final String SERVICE_ACTION_RESUME = "continueWorkerThread";
    public static final String SERVICE_ACTION_PAUSE = "pauseWorkerThread";
    public static final String SERVICE_ACTION_KILL = "killWorkerThread";
    public static final String SERVICE_ACTION_ACTIVITY_KILLED = "activityDestroyed";
    public static final String SERVICE_ACTION_REQUEST_KEY = "getNewUID";
    public static final String SERVICE_ACTION_REQUEST_SYNC = "syncToDB";
    public static final String SERVICE_ACTION_REQUEST_RATES = "downloadRates";
    public static final String SERVICE_ACTION_REQUEST_DOWNLOAD = "downloadTrackers";
    public static final String SERVICE_ACTION_REQUEST_UPLOAD = "uploadTrackers";

    public static final String SERVICE_BROADCAST_DOWNLOAD_FAILED = "com.alon.exchangetracker.TrackerDownloadFailed";
    public static final String SERVICE_BROADCAST_DOWNLOAD_WORKED = "com.alon.exchangetracker.TrackerDownloadWorked";

    public static final int SERVICE_HANDLER_SAVE_KEY_AND_UID = 5021;
    public static final int SERVICE_HANDLER_NOTIFY = 2346;
    public static final int SERVICE_HANDLER_MIM = 9425;
    public static final int SERVICE_HANDLER_DOWNLOAD_STATE_CHANGED = 6012;

    public static final int ACTIVITY_HANDLER_RELOAD_DATASET = 1639;

    public static final String SHARED_PREFERENCES_NAME = "exchangeTrackerSharedPrefs";
    public static final String SHARED_PREFERENCES_UID = "deviceUID";
    public static final String SHARED_PREFERENCES_UID_KILL = "lastValidDate";
    public static final String SHARED_PREFERENCES_FIRST_RUN = "isFirstRun";
    public static final String SHARED_PREFERENCES_TRACKER_VERSION = "lastDatabasePullVersion";
    public static final String SHARED_PREFERENCES_AUTO_UPLOAD = "autoUpload";
}

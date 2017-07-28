package com.alon.exchangetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_INIT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_RESUME;

/**
 * Created by Alon on 6/30/2017.
 */

public class ConnectionStateChange extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ExchangeTrackerUtility util = ExchangeTrackerUtility.getInstance();
            if (util.hasConnection(context)) {
                Toast.makeText(context, context.getString(R.string.gotConnection), Toast.LENGTH_SHORT).show();
                String action = SERVICE_ACTION_INIT;
                if (!ExchangeTrackerUtility.getInstance().isFirstRun(context))
                    if (new File(context.getFilesDir(), "pKey").exists()) {
                        Log.i("ConnectionStateChange", "initService: Key exists");
                        if (util.getUIDTermination(context) > System.currentTimeMillis() ||
                                util.getUID(context) != null) {
                            Log.i("ConnectionStateChange", "initService: setting action to resume");
                            action = SERVICE_ACTION_RESUME;
                        }
                    }
                Intent service = new Intent(context, ExchangeTrackerService.class);
                service.setAction(action);
                context.startService(service);
                util.setFirstRun(context, false);
                context.unregisterReceiver(this);
            }
        }
    }
}

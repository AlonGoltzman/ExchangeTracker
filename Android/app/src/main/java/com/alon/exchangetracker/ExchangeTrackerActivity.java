package com.alon.exchangetracker;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.alon.exchangetracker.Trackers.TRACKERS_FILE_NAME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTIVITY_HANDLER_RELOAD_DATASET;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_ACTIVITY_KILLED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_INIT;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_DOWNLOAD;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_RATES;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_REQUEST_UPLOAD;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_ACTION_RESUME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_BROADCAST_DOWNLOAD_FAILED;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SERVICE_BROADCAST_DOWNLOAD_WORKED;

public class ExchangeTrackerActivity extends Activity implements View.OnClickListener, TrackerTouchHelper.TouchListener, PopupMenu.OnMenuItemClickListener {

    private ExchangeTrackerUtility mUtil;
    private ExchangeTrackerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        findViewById(R.id.tracker_addNewTracker).setOnClickListener(this);
        findViewById(R.id.tracker_actionMenu).setOnClickListener(this);

        mUtil = ExchangeTrackerUtility.getInstance();
        adapter = new ExchangeTrackerListAdapter();
        RecyclerView listView = findViewById(R.id.tracker_List);
        TrackerSwipeHelper helper = new TrackerSwipeHelper();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) != PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, 123);
                return;
            }
        }
        if (mUtil.hasConnection(this))
            initService();
        else {
            Toast.makeText(this, getString(R.string.noConnection), Toast.LENGTH_SHORT).show();
            registerReceiver(new ConnectionStateChange(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        adapter.setMainThreadId(Thread.currentThread().getId());

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);
        listView.addOnItemTouchListener(new TrackerTouchHelper(this));

        helper.attachToRecyclerView(listView);

        Trackers.load(new File(getFilesDir(), TRACKERS_FILE_NAME));
        Trackers.getInstance().setListener(adapter);
        Trackers.getInstance().setMainThreadHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == ACTIVITY_HANDLER_RELOAD_DATASET) {
                    adapter.updated();
                }
            }
        });
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 123)
            if (grantResults[0] == PERMISSION_GRANTED && (grantResults.length <= 1 || grantResults[1] == PERMISSION_GRANTED))
                initService();
            else
                Toast.makeText(this, getString(R.string.needsInternetPermission), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tracker_addNewTracker) {
            showFragment(null);
        } else if (v.getId() == R.id.tracker_actionMenu) {
            PopupMenu menu = new PopupMenu(this, v);
            MenuInflater inflater = menu.getMenuInflater();
            inflater.inflate(R.menu.action_menu, menu.getMenu());
            menu.show();
            menu.setOnMenuItemClickListener(this);
            menu.getMenu().getItem(menu.getMenu().size() - 1).setChecked(mUtil.shouldAutoUpload(this));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, ExchangeTrackerService.class);
        intent.setAction(SERVICE_ACTION_ACTIVITY_KILLED);
        startService(intent);
    }

    @Override
    public void onTouch(View clickedView, int adapterPosition) {
        showFragment(Trackers.getInstance().getTracker(adapterPosition));
    }

    private void initService() {
        String action = SERVICE_ACTION_INIT;
        if (!mUtil.isFirstRun(this))
            if (new File(getFilesDir(), "pKey").exists()) {
                Log.i("TrackerActivity", "initService: Key exists");
                if (mUtil.getUIDTermination(this) > System.currentTimeMillis() ||
                        mUtil.getUID(this) != null) {
                    Log.i("TrackerActivity", "initService: setting action to resume");
                    action = SERVICE_ACTION_RESUME;
                }
            }
        Intent service = new Intent(this, ExchangeTrackerService.class);
        service.setAction(action);
        startService(service);
        mUtil.setFirstRun(this, false);
    }

    private void showFragment(Tracker tracker) {
        FragmentManager manager = getFragmentManager();
        ExchangeTrackerFragment fragment = new ExchangeTrackerFragment();
        fragment.setCancelable(false);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        if (tracker != null)
            fragment.setCreationTracker(tracker);
        fragment.show(manager, "");
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent service = new Intent(this, ExchangeTrackerService.class);
        switch (item.getItemId()) {
            case R.id.action_check_rates:
                service.setAction(SERVICE_ACTION_REQUEST_RATES);
                break;
            case R.id.action_download:
                try {
                    Trackers.getInstance().save(new File(getFilesDir(), TRACKERS_FILE_NAME));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BroadcastReceiver failedReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(SERVICE_BROADCAST_DOWNLOAD_FAILED)) {
                            Trackers.load(new File(context.getFilesDir(), TRACKERS_FILE_NAME));
                            context.unregisterReceiver(this);
                        } else
                            context.unregisterReceiver(this);
                    }
                };
                IntentFilter filter = new IntentFilter();
                filter.addAction(SERVICE_BROADCAST_DOWNLOAD_FAILED);
                filter.addAction(SERVICE_BROADCAST_DOWNLOAD_WORKED);
                registerReceiver(failedReceiver, filter);
                service.setAction(SERVICE_ACTION_REQUEST_DOWNLOAD);
                Trackers.getInstance().clear();
                break;
            case R.id.action_upload:
                service.setAction(SERVICE_ACTION_REQUEST_UPLOAD);
                break;
            case R.id.action_auto_upload:
                boolean checked = !item.isChecked();
                item.setChecked(checked);
                mUtil.setAutoUpload(this, checked);
                return true;
            default:
                return false;
        }
        startService(service);
        return true;
    }
}

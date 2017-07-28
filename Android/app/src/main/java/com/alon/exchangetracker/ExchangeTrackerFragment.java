package com.alon.exchangetracker;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

import static com.alon.exchangetracker.ExchangeTrackerThread.BASE_URL;
import static com.alon.exchangetracker.ExchangeTrackerUtility.openConnection;
import static com.alon.exchangetracker.ExchangeTrackerUtility.processConnection;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_INSERT_TRACKERS;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.ACTION_UPDATE_TRACKERS;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.OK;

public class ExchangeTrackerFragment extends DialogFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {


    private Spinner baseCurrency;
    private Spinner foreignCurrency;
    private EditText sum;
    private ToggleButton notify;
    private EditText notifyAmount;

    private Tracker creationTracker;
    private Trackers trackers;


    private TextInputLayout sumLayout;
    private TextInputLayout notifyAmountLayout;

    @SuppressLint("DefaultLocale")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_tracker_info, container, false);

        ((TextView) view.findViewById(R.id.trackerInfo_title)).setText(creationTracker == null ? getString(R.string.creating) : getString(R.string.updating));

        baseCurrency = view.findViewById(R.id.trackerInfo_coinType);
        foreignCurrency = view.findViewById(R.id.trackerInfo_conversionType);
        sum = view.findViewById(R.id.trackerInfo_conversionAmount);
        notify = view.findViewById(R.id.trackerInfo_notify);
        notifyAmount = view.findViewById(R.id.trackerInfo_AmountToNotifyOver);

        sumLayout = view.findViewById(R.id.trackerInfo_sumLayout);
        notifyAmountLayout = view.findViewById(R.id.trackerInfo_notifyLayout);

        Button cancel = view.findViewById(R.id.trackerInfo_dismissTracker);
        Button save = view.findViewById(R.id.trackerInfo_saveTracker);

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
        notify.setOnCheckedChangeListener(this);

        if (creationTracker != null) {
            ArrayList<String> currencies = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.currencies)));
            baseCurrency.setSelection(currencies.indexOf(creationTracker.getBase()));
            foreignCurrency.setSelection(currencies.indexOf(creationTracker.getForeign()));
            sum.setText(String.format("%.0f", creationTracker.getSum()));
            notify.setChecked(creationTracker.getNotify());
            if (notify.isChecked())
                notifyAmount.setText(String.format("%.0f", creationTracker.getNotifySum()));
        }
        trackers = Trackers.getInstance();
        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.trackerInfo_notify)
            notifyAmount.setEnabled(isChecked);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.trackerInfo_dismissTracker) {
            dismiss();
            return;
        }
        if (v.getId() == R.id.trackerInfo_saveTracker) {
            if (!validate())
                return;
            String base = (String) baseCurrency.getSelectedItem();
            String foreign = (String) foreignCurrency.getSelectedItem();
            long currentSum = Long.parseLong(sum.getText().toString());
            boolean shouldNotify = notify.isChecked();
            long sumToNotify = notifyAmount.getText() == null ? -1 : notifyAmount.getText().toString().isEmpty() ? -1 : Long.parseLong(notifyAmount.getText().toString());
            Tracker newTracker = new Tracker(base, foreign, (double) currentSum, shouldNotify, (double) sumToNotify);

            if (ExchangeTrackerUtility.getInstance().shouldAutoUpload(getActivity().getApplicationContext())) {
                try {
                    String encryptedRequest = new ExchangeTrackerQueryBuilder()
                            .begin()
                            .action(creationTracker == null ? ACTION_INSERT_TRACKERS : ACTION_UPDATE_TRACKERS)
                            .uid(ExchangeTrackerUtility.getInstance().getUID(getActivity()))
                            .version(-1d)
                            .initInformationPour()
                            .openNewTracker(base)
                            .setForeignCurrencies(foreign)
                            .setSums((double) currentSum)
                            .setNotifications(shouldNotify)
                            .setNotificationSums((double) sumToNotify)
                            .closeTracker()
                            .finishInformationPour()
                            .finishQueryGenerationWithEncryption(ExchangeTrackerUtility.getInstance().getPublicKey(getActivity()));
                    new TrackerUpdateAsyncTask().execute(encryptedRequest, newTracker);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (creationTracker == null)
                    trackers.addTracker(newTracker);
                else
                    trackers.setTracker(creationTracker, newTracker);
                dismiss();
            }
        }
    }

    void setCreationTracker(Tracker creationTracker) {
        this.creationTracker = creationTracker;
    }

    private boolean validate() {
        if (foreignCurrency.getSelectedItemPosition() == baseCurrency.getSelectedItemPosition()) {
            ((TextView) foreignCurrency.getSelectedView()).setError("Foreign and Base Currencies should be different.");
            return false;
        }
        if (Long.parseLong(sum.getText().toString()) > 1000000000) {
            sumLayout.setError("Must be under 1,000,000,000");
            return false;
        }
        if (notify.isChecked())
            if (Long.parseLong(notifyAmount.getText().toString()) > 1000000000) {
                notifyAmountLayout.setError("Must be under 1,000,000,000");
                return false;
            }
        return true;
    }

    private class TrackerUpdateAsyncTask extends AsyncTask<Object, Void, Boolean> {

        private Tracker trackerToAdd;

        @Override
        protected Boolean doInBackground(Object... objs) {
            trackerToAdd = (Tracker) objs[1];
            return processConnection(openConnection(BASE_URL), objs[0].toString()).equals(OK);
        }


        @Override
        protected void onPostExecute(Boolean flag) {
            if (!flag) {
                Toast.makeText(getActivity(), getString(R.string.failedInsertingTrackers), Toast.LENGTH_SHORT).show();
            } else {
                if (creationTracker == null)
                    trackers.addTracker(trackerToAdd);
                else
                    trackers.setTracker(creationTracker, trackerToAdd);
                dismiss();
            }
        }
    }

}

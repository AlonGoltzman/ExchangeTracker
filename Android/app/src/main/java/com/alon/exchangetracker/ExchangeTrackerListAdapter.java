package com.alon.exchangetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Alon on 6/26/2017.
 */

class ExchangeTrackerListAdapter extends RecyclerView.Adapter<ExchangeTrackerListAdapter.TrackerViewHolder> implements TrackerUpdateListener {


    private long mainThreadId;

    void setMainThreadId(long id) {
        mainThreadId = id;
    }

    @Override
    public TrackerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listview_row, parent, false);
        return new TrackerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TrackerViewHolder holder, int position) {
        Tracker tracker = Trackers.getInstance().getTracker(position);
        Context context = holder.getCurrencyInformation().getContext();
        String text = context.getString(R.string.conversionRowText, tracker.getBase(), tracker.getForeign());
        @SuppressLint("DefaultLocale") String sum = String.format("%.0f", tracker.getSum());
        holder.getCurrencyInformation().setText(text);
        holder.getSum().setText(sum);
    }

    @Override
    public int getItemCount() {
        return Trackers.getInstance().size();
    }

    @Override
    public boolean updated() {
        if (Thread.currentThread().getId() == mainThreadId)
            notifyDataSetChanged();
        else {
            Log.i("ListAdapter", "updated: tried notifying data set changed but not on ui thread.");
            return false;
        }
        return true;
    }

    class TrackerViewHolder extends RecyclerView.ViewHolder {

        private TextView currencyInformation;
        private TextView sum;

        TrackerViewHolder(View container) {
            super(container);
            currencyInformation = container.findViewById(R.id.tracker_trackerInfoText);
            sum = container.findViewById(R.id.tracker_trackerInfoSum);
        }

        TextView getCurrencyInformation() {
            return currencyInformation;
        }

        TextView getSum() {
            return sum;
        }
    }
}

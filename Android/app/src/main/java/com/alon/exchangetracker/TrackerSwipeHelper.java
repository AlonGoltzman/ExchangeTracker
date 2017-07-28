package com.alon.exchangetracker;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Created by Alon on 6/27/2017.
 */

class TrackerSwipeHelper extends ItemTouchHelper {

    TrackerSwipeHelper() {
        super(new SwipeCallback());
    }

    private static class SwipeCallback extends Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            Trackers.remove(viewHolder.getAdapterPosition());
        }

    }
}

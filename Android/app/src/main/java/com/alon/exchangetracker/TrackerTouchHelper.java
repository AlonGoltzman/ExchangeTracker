package com.alon.exchangetracker;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Alon on 6/27/2017.
 */

class TrackerTouchHelper implements RecyclerView.OnItemTouchListener {
    private TouchListener listener;
    private boolean moved = false;

    TrackerTouchHelper(TouchListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View clickedView = rv.findChildViewUnder(e.getX(), e.getY());
        if (e.getAction() == MotionEvent.ACTION_UP && !moved)
            if (clickedView != null)
                if (listener != null) {
                    listener.onTouch(clickedView, rv.getChildAdapterPosition(clickedView));
                    return true;
                }
        if (e.getAction() == MotionEvent.ACTION_UP)
            moved = false;
        if (e.getAction() == MotionEvent.ACTION_MOVE)
            moved = true;
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    interface TouchListener {
        void onTouch(View clickedView, int adapterPosition);
    }
}

package com.alon.exchangetracker;

import java.io.Serializable;

/**
 * Created by Alon on 6/24/2017.
 */
@SuppressWarnings("WeakerAccess")
class Tracker implements Serializable {

    private String base;
    private String foreign;
    private Double sum;
    private Boolean notify;
    private Double notifySum;

    void setBase(String base) {
        this.base = base;
    }

    void setForeign(String foreign) {
        this.foreign = foreign;
    }

    void setSum(Double sum) {
        this.sum = sum;
    }

    void setNotify(Boolean notify) {
        this.notify = notify;
    }

    void setNotifySum(Double notifySum) {
        this.notifySum = notifySum;
    }


    Tracker(String b, String f, Double s, Boolean n, Double nS) {
        setBase(b);
        setForeign(f);
        setSum(s);
        setNotify(n);
        setNotifySum(nS);
    }

    String getBase() {
        return base;
    }

    String getForeign() {
        return foreign;
    }

    Double getSum() {
        return sum;
    }

    Boolean getNotify() {
        return notify;
    }

    Double getNotifySum() {
        return notifySum;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Tracker &&
                ((Tracker) obj).getBase().equals(getBase()) &&
                ((Tracker) obj).getForeign().equals(getForeign()) &&
                ((Tracker) obj).getSum().equals(getSum()) &&
                ((Tracker) obj).getNotify().equals(getNotify()) &&
                ((Tracker) obj).getNotifySum().equals(getNotifySum());
    }
}

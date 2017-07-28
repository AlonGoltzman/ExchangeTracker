package com.alon.exchangetrackerserver;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by Alon on 6/23/2017.
 */
public class ExchangeTrackerUIDList {

    private static Set<UID> uids;
    private static Timer terminationTimer;
    private static LockedTimerTask terminationTask;

    static {
        if (uids == null)
            uids = new HashSet<>();
        terminationTask = new LockedTimerTask() {
            @Override
            public void run() {
                if (canExecute()) {
                    ExchangeTrackerUIDList.terminateUIDs();
                    super.run();
                    reschedule();
                }
            }

        };
    }


    //Possible clashes.
    static synchronized String addNewUID() {
        terminateUIDs();
        String uid = new BigInteger(128, new SecureRandom()).toString(16);
        while (UIDExists(uid))
            uid = new BigInteger(128, new SecureRandom()).toString(16);
        UID newUID = new UID(uid);
        uids.add(newUID);
        return uid;
    }

    static void start() {
        if (terminationTimer == null) {
            terminationTimer = new Timer();
            terminationTimer.schedule(terminationTask, Date.from(LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
    }

    static void stop() {
        if (terminationTimer != null) {
            terminationTimer.cancel();
            if (terminationTask.isAssigned())
                terminationTask.dontExecute();
            terminationTimer.purge();
            terminationTimer = null;
        }
    }

    private static void reschedule() {
        if (terminationTimer != null)
            if (!terminationTask.isAssigned())
                terminationTimer.schedule(terminationTask, Date.from(LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private synchronized static void terminateUIDs() {
        for (UID uid : uids)
            if (uid.shouldBeTerminated())
                uids.remove(uid);
    }

    static boolean UIDExists(String uid) {
        System.out.println("=========================================");
        System.out.println("Size: " + uids.size());
        System.out.println("Target: " + uid);
        for (UID uidObj : uids) {
            System.out.println("Element: " + uidObj.toString());
            if (uidObj.getUID().equals(uid))
                return true;
        }
        return false;
    }

    private static class UID {
        private String uid;
        private LocalDate terminationTime;

        UID(String id) {
            uid = id;
            terminationTime = LocalDate.now().plusMonths(1);
        }

        boolean shouldBeTerminated() {
            return terminationTime.isBefore(LocalDate.now());
        }

        String getUID() {
            return uid;
        }

        @Override
        public String toString() {
            return uid;
        }
    }

    private static class LockedTimerTask extends TimerTask {
        private boolean isAssigned = false;
        private boolean canExecute = true;

        LockedTimerTask() {
            isAssigned = true;
        }

        @Override
        public void run() {
            isAssigned = false;
        }

        boolean isAssigned() {
            return isAssigned;
        }

        boolean canExecute() {
            return canExecute;
        }

        void dontExecute() {
            canExecute = false;
        }
    }
}

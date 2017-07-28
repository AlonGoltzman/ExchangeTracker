package com.alon.exchangetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_AUTO_UPLOAD;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_FIRST_RUN;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_NAME;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_UID;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.SHARED_PREFERENCES_UID_KILL;

final class ExchangeTrackerUtility {

    private PublicKey publicKey;
    private String uid;
    private SharedPreferences prefs;

    private static ExchangeTrackerUtility instance;

    static ExchangeTrackerUtility getInstance() {
        return instance == null ? new ExchangeTrackerUtility() : instance;
    }

    private ExchangeTrackerUtility() {
        instance = this;
    }

    //============================================

    boolean hasConnection(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mgr.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    //============================================

    boolean isFirstRun(Context context) {
        ensureSharedPreferencesExist(context);
        return prefs.getBoolean(SHARED_PREFERENCES_FIRST_RUN, true);
    }

    void setFirstRun(Context context, boolean flag) {
        ensureSharedPreferencesExist(context);
        prefs.edit().putBoolean(SHARED_PREFERENCES_FIRST_RUN, flag).apply();
    }

    //============================================

    void setAutoUpload(Context context, boolean flag) {
        ensureSharedPreferencesExist(context);
        prefs.edit().putBoolean(SHARED_PREFERENCES_AUTO_UPLOAD, flag).apply();
    }

    boolean shouldAutoUpload(Context context) {
        ensureSharedPreferencesExist(context);
        return prefs.getBoolean(SHARED_PREFERENCES_AUTO_UPLOAD, false);
    }
    //============================================


    String getUID() {
        return uid;
    }

    String getUID(Context context) {
        ensureSharedPreferencesExist(context);
        return uid = prefs.getString(SHARED_PREFERENCES_UID, null);
    }

    long getUIDTermination(Context context) {
        ensureSharedPreferencesExist(context);
        return prefs.getLong(SHARED_PREFERENCES_UID_KILL, 0);
    }


    boolean wasUIDTerminated(Context context) {
        ensureSharedPreferencesExist(context);
        long terminationTime = prefs.getLong(SHARED_PREFERENCES_UID_KILL, 0);
        return terminationTime < System.currentTimeMillis();
    }
    //============================================

    PublicKey getPublicKey() {
        return publicKey;
    }

    PublicKey getPublicKey(Context context) {
        if (publicKey == null)
            try {
                if (!loadPublicKey(context))
                    return null;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        return publicKey;
    }

    private boolean loadPublicKey(Context context) throws IOException, ClassNotFoundException {
        File keyFile = new File(context.getFilesDir(), "pKey");
        if (keyFile.exists()) {
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(keyFile));
            publicKey = (PublicKey) stream.readObject();
            return true;
        }
        return false;
    }

    //============================================

    private void ensureSharedPreferencesExist(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_APPEND);
    }
    //============================================

    static String encrypt(String input, Key encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            return new String(org.apache.commons.codec.binary.Base64.encodeBase64(cipher.doFinal(input.getBytes("UTF-8"))));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String decrypt(String input, Key decryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
            return new String(cipher.doFinal(org.apache.commons.codec.binary.Base64.decodeBase64(input.getBytes())), "UTF-8");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | RuntimeException e) {
            Log.e("Utility", "decrypt: Possible error decrypting: " + input, e);
            return input;
        }
    }

    //============================================

    static Trackers reloadTrackers(Trackers trackers, File trackersFile) {
        TrackerUpdateListener listener = trackers.getListener();
        Trackers newTrackers = Trackers.load(trackersFile);
        newTrackers.setListener(listener);
        return newTrackers;
    }

    //============================================

    static HttpURLConnection openConnection(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return conn;
    }

    //============================================

    static String processConnection(HttpURLConnection conn) {
        return processConnection(conn, 1024, null);
    }

    static String processConnection(HttpURLConnection conn, int bufferSize) {
        return processConnection(conn, bufferSize, null);
    }

    static String processConnection(HttpURLConnection conn, String output) {
        return processConnection(conn, 1024, output);
    }

    //============================================

    private static String processConnection(HttpURLConnection conn, int bufferSize, String output) {
        try {
            conn.connect();

            if (output != null) {
                OutputStream outStream = conn.getOutputStream();
                outStream.write(output.getBytes());
                outStream.flush();
                outStream.close();
            }

            InputStream inStream = conn.getInputStream();

            int aRead;
            byte[] buffer = new byte[bufferSize];
            StringBuilder builder = new StringBuilder();

            while ((aRead = inStream.read(buffer)) != -1)
                builder.append(new String(buffer, 0, aRead));

            inStream.close();
            conn.disconnect();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

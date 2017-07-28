package com.alon.exchangetrackerserver;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletContext;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.LOG_ERROR;
import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.LOG_INFO;

/**
 * Created by Alon on 6/23/2017.
 */
public class ExchangeTrackerUtility {

    private static PrivateKey pKey;


    static PrivateKey loadKey(ServletContext context) {
        if (pKey == null)
            try {
                ObjectInputStream stream = new ObjectInputStream(new FileInputStream(new File(context.getRealPath(""), "PrivateKey.key")));
                pKey = (PrivateKey) stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        return pKey;
    }

    static void Log(int logType, String... msgs) {
        switch (logType) {
            case LOG_ERROR:
                for (String msg : msgs)
                    System.err.println("[ERROR - " + System.currentTimeMillis() + "] \t " + msg);

                break;
            case LOG_INFO:
                for (String msg : msgs)
                    System.out.println("[INFO - " + System.currentTimeMillis() + "] \t " + msg);
                break;
        }
    }

    static String stackTraceAsString(StackTraceElement[] elems) {
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement elem : elems)
            trace.append(elem.toString()).append("\n");
        return trace.toString();
    }

    static boolean ensureOS(String header) {
        return header.toLowerCase().contains("android");
    }

    static String encrypt(String input, Key encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            return Base64.encodeBase64String(cipher.doFinal(input.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String decrypt(String input, Key decryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
            return new String(cipher.doFinal(Base64.decodeBase64(input.getBytes())), "UTF-8");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }
}

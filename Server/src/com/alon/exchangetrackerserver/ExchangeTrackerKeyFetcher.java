package com.alon.exchangetrackerserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class ExchangeTrackerKeyFetcher extends javax.servlet.http.HttpServlet {
    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        OutputStream stream = response.getOutputStream();
        sendPublicKey(stream);
    }

    protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        OutputStream stream = response.getOutputStream();
        sendPublicKey(stream);
    }

    private void sendPublicKey(OutputStream stream) {
        try {
            File publicKey = new File(getServletContext().getRealPath(""), "PublicKey.key");
            byte[] keyArray = Files.readAllBytes(publicKey.toPath());
            stream.write(keyArray);
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

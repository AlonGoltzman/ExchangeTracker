package com.alon.exchangetrackerserver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.ensureOS;

/**
 * Created by Alon on 6/23/2017.
 */

public class ExchangeTrackerKeyHash extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureOS(request.getHeader("User-Agent"))) {
            response.sendError(400);
            return;
        }
        sendHash(response.getWriter());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureOS(request.getHeader("User-Agent"))) {
            response.sendError(400);
            return;
        }
        sendHash(response.getWriter());
    }

    private void sendHash(PrintWriter stream) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] response = md.digest(Files.readAllBytes(new File(getServletContext().getRealPath(""), "PublicKey.key").toPath()));
            StringBuilder sb = new StringBuilder();
            for (byte aResponse : response) sb.append(Integer.toString((aResponse & 0xff) + 0x100, 16).substring(1));
            String hash = sb.toString();
            stream.write(hash);
            stream.flush();
            stream.close();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}

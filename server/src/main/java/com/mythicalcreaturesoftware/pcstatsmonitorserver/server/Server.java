package com.mythicalcreaturesoftware.pcstatsmonitorserver.server;

import com.mythicalcreaturesoftware.pcstatsmonitorserver.scanner.StatScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class Server implements Runnable {

    private static final Logger LOG = LogManager.getLogger(Server.class);

    private Socket socket;

    public Server(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        LOG.info("Connected: " + socket);
        try {
            Scanner in = new Scanner(socket.getInputStream());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                LOG.debug("Reading line " + in.nextLine());

                Map<String, String> stats = StatScanner.getInstance().getStatsInfo();
                JSONObject statsJson = new JSONObject(stats);

                out.println(statsJson.toString());
            }

        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
            LOG.info("Closed: " + socket);
        }
    }
}
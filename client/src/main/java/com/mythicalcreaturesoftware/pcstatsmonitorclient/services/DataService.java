package com.mythicalcreaturesoftware.pcstatsmonitorclient.services;

import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;

public class DataService extends Service<Void> {

    private static final Logger LOG = LogManager.getLogger(DataService.class);

    private static int MAXIMUM_TIME_OUT = 300000;

    @Override
    protected Task<Void> createTask() {

        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                LOG.info("Connecting with data server...");

                InetAddress serverIp = searchServer(100);

                try (Socket socket = new Socket(serverIp, Keys.SERVER_PORT)) {
                    LOG.info("Connection successful");

                    Scanner in = new Scanner(socket.getInputStream());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("initial_data_request");

                    while (in.hasNextLine()) {
                        LOG.debug("Requesting next data");

                        if (isCancelled()) {
                            break;
                        }

                        String line = in.nextLine();
                        updateMessage(line);

                        Thread.sleep(1000);

                        out.println("data_request");
                    }

                }

                return null;
            }
        };
    }

    private InetAddress searchServer(int timeout) {
        LOG.info("Searching stats server");
        InetAddress serverIp =null;

        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);

            byte[] sendData = Keys.DISCOVER_STATS_REQUEST.getBytes();

            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), Keys.DISCOVERY_SERVER_PORT);
                datagramSocket.send(sendPacket);
                LOG.info("Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }

            sendRequestToNetworkInterfaces(datagramSocket, sendData);

            LOG.info("Done looping over all network interfaces. Now waiting for a reply");

            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            datagramSocket.setSoTimeout(timeout);
            datagramSocket.receive(receivePacket);

            LOG.info("Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            String message = new String(receivePacket.getData()).trim();
            if (message.equals(Keys.DISCOVER_STATS_RESPONSE)) {
                serverIp = receivePacket.getAddress();
            }

            datagramSocket.close();
        } catch (SocketTimeoutException e) {
            LOG.info("Client timed out, trying again...");

            if (timeout < MAXIMUM_TIME_OUT) {
                serverIp = searchServer(timeout * 2);
            } else {
                LOG.error("Unable to connect to server");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return serverIp;
    }

    private void sendRequestToNetworkInterfaces (DatagramSocket datagramSocket, byte[] sendData) throws SocketException {
        LOG.debug("Sending request to network interfaces");

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {

            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }

                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, Keys.DISCOVERY_SERVER_PORT);
                    datagramSocket.send(sendPacket);
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }

                LOG.debug("Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
            }
        }
    }
}

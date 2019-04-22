package com.mythicalcreaturesoftware.pcstatsmonitorserver.server;

import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryServer implements Runnable {

    private static final Logger LOG = LogManager.getLogger(Server.class);

    private static DiscoveryServer SINGLE_INSTANCE = null;

    @Override
    public void run() {
        LOG.debug("Starting discovery server");

        try {
            DatagramSocket socket = new DatagramSocket(Keys.DISCOVERY_SERVER_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (true) {
                LOG.debug("Ready to receive broadcast packets");

                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

                socket.receive(packet);

                LOG.debug("Discovery packet received from: " + packet.getAddress().getHostAddress());
                LOG.debug("Packet received; data: " + new String(packet.getData()));

                String message = new String(packet.getData()).trim();
                if (message.equals(Keys.DISCOVER_STATS_REQUEST)) {

                    byte[] sendData = Keys.DISCOVER_STATS_RESPONSE.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    LOG.debug("Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public static DiscoveryServer getInstance() {
        if (SINGLE_INSTANCE == null) {
            synchronized(DiscoveryServer.class) {
                SINGLE_INSTANCE = new DiscoveryServer();
            }
        }
        return SINGLE_INSTANCE;
    }
}

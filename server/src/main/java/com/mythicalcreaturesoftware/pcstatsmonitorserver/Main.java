package com.mythicalcreaturesoftware.pcstatsmonitorserver;

import com.mythicalcreaturesoftware.pcstatsmonitorserver.server.DiscoveryServer;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.server.Server;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys.DEBUG;

public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String [] args) {
        for (String arg : args) {
            processArg(arg);
        }

        setLibraryPath();

        Thread discoveryThread = new Thread(DiscoveryServer.getInstance());
        discoveryThread.start();

        try (ServerSocket listener = new ServerSocket(Keys.SERVER_PORT)) {
            LOG.info("The stat monitor server is running...");
            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new Server(listener.accept()));
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private static void processArg(String arg) {
        switch(arg) {
            case DEBUG:
                setDebugLoggerLevel();
                break;
            default:
        }
    }

    private static void setDebugLoggerLevel() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.DEBUG);
        context.updateLoggers();
    }

    private static void setLibraryPath () {
        String path = "";

        try {
            String protocol = Main.class.getResource("").getProtocol();
            if(Objects.equals(protocol, "jar")){
                File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                path =  file.getParentFile().getAbsolutePath() + "/libs";
            } else if(Objects.equals(protocol, "file")) {
                path = Main.class.getResource("/libs").getPath();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.setProperty("java.library.path", path);
        System.setProperty("jna.library.path", path  + "/jna" );
    }
}

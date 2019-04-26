package com.mythicalcreaturesoftware.pcstatsmonitorclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;

import static com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.ClientKeys.DEBUG;
import static com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.ClientKeys.FULLSCREEN;

public class Main extends Application {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    private static boolean IS_FULLSCREEN = false;

    @Override
    public void start(Stage stage) {
        try {
            LOG.info("Loading UI");

            Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            stage.setTitle("PC Stats Monitor");
            stage.setScene(scene);
            stage.setFullScreen(IS_FULLSCREEN);
            stage.show();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String [] args) {
        LOG.info("Starting application...");

        for (String arg : args) {
            processArg(arg);
        }

        launch();
    }

    private static void setDebugLoggerLevel() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.DEBUG);
        context.updateLoggers();
    }

    private static void processArg(String arg) {
        switch(arg) {
            case FULLSCREEN:
                IS_FULLSCREEN = true;
                break;
            case DEBUG:
                setDebugLoggerLevel();
                break;
            default:
        }
    }
}
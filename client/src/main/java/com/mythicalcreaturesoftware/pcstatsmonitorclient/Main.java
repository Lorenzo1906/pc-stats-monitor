package com.mythicalcreaturesoftware.pcstatsmonitorclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Main extends Application {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage stage) {
        try {
            LOG.info("Loading UI");

            Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            stage.setTitle("PC Stats Monitor");
            stage.setScene(scene);
            //stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String [] args) {

        LOG.info("Starting application...");
        launch();
    }
}
package com.mythicalcreaturesoftware.pcstatsmonitorclient.controllers;

import com.mythicalcreaturesoftware.pcstatsmonitorclient.services.DataService;
import com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.ClientKeys;
import com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.DataUtils;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import eu.hansolo.medusa.Gauge;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.xml.crypto.Data;

public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class);

    @FXML
    private VBox mainContent;

    @FXML
    private BorderPane messageContent;

    @FXML
    private Label cpuName;

    @FXML
    private Label gpuName;

    @FXML
    private Label message;

    @FXML
    private Gauge cpuUsage;

    @FXML
    private Gauge cpuTemp;

    @FXML
    private Gauge gpuUsage;

    @FXML
    private Gauge diskUsage;

    @FXML
    private Gauge gpuTemp;

    @FXML
    private Gauge memUsed;

    private DataService dataService;
    private StringProperty statusMessagesProperty;

    public void initialize() {
        setUpService();

        dataService.messageProperty().addListener(
            (ObservableValue<? extends Object> observable, Object oldValue, Object newValue)-> {

                if (newValue.equals(ClientKeys.CONNECTING)) {
                    LOG.debug("Showing connect message");

                    showMessagePane(true);
                } else {
                    LOG.debug("Updating UI data");

                    showMessagePane(false);
                    processUIData(newValue.toString());
                }
            }
        );

        dataService.start();
    }

    private void showMessagePane (boolean value) {
        mainContent.setVisible(!value);
        messageContent.setVisible(value);
    }

    private void processUIData (String data) {
        if (!DataUtils.isJSONValid(data)) {
            return;
        }

        JSONObject json = new JSONObject(data);

        String cpuNameData = json.has(Keys.CPU_NAME) ? json.getString(Keys.CPU_NAME) : "";
        String cpuUsageData = json.has(Keys.CPU_USAGE) ? json.getString(Keys.CPU_USAGE) : "0";
        String cpuTempData = json.has(Keys.CPU_TEMP) ? json.getString(Keys.CPU_TEMP) : "0";
        String gpuNameData = json.has(Keys.GPU_NAME) ? json.getString(Keys.GPU_NAME) : "";
        String gpuUsageData = json.has(Keys.GPU_USAGE) ? json.getString(Keys.GPU_USAGE) : "0";
        String gpuTempData = json.has(Keys.GPU_TEMP) ? json.getString(Keys.GPU_TEMP) : "0";
        String gpuShutdown = json.has(Keys.GPU_SHUTDOWN) ? json.getString(Keys.GPU_SHUTDOWN) : "0";
        String diskTotalData = json.has(Keys.DISK_TOTAL) ? json.getString(Keys.DISK_TOTAL) : "0";
        String diskUsedData = json.has(Keys.DISK_USED) ? json.getString(Keys.DISK_USED) : "0";
        String memTotalData = json.has(Keys.MEM_TOTAL) ? json.getString(Keys.MEM_TOTAL) : "0";
        String memAvailableData = json.has(Keys.MEM_AVAILABLE) ? json.getString(Keys.MEM_AVAILABLE) : "0";

        cpuName.textProperty().setValue(cpuNameData);
        gpuName.textProperty().setValue(gpuNameData);

        cpuUsage.setValue(Double.parseDouble(cpuUsageData));
        cpuTemp.setValue(Double.parseDouble(cpuTempData.replace("°C","").trim()));

        gpuUsage.setValue(Double.parseDouble(gpuUsageData.replace("%", "").trim()));

        diskUsage.setValue(DataUtils.calculatePercentage(Double.parseDouble(diskTotalData.trim()), Double.parseDouble(diskUsedData.trim())));
        gpuTemp.setMaxValue(Double.parseDouble(gpuShutdown.replace("C","").trim()));
        gpuTemp.setValue(Double.parseDouble(gpuTempData.replace("C","").trim()));

        double memoryAvailable = DataUtils.kilobytesToMegabytes(memAvailableData);
        double memoryTotal = DataUtils.kilobytesToMegabytes(memTotalData);
        memUsed.setValue(DataUtils.calculatePercentage(memoryTotal, memoryTotal - memoryAvailable));
    }

    private void setUpService () {
        LOG.info("Starting data service...");

        dataService = new DataService();
        dataService.setOnSucceeded((WorkerStateEvent t) -> {
            LOG.debug("Disconnected from server");

            dataService.restart();
        });
        dataService.setOnRunning((WorkerStateEvent t) -> LOG.debug("Calling data service"));
        dataService.setOnFailed((WorkerStateEvent t) -> {
            LOG.debug("Connection lost to server");

            showMessagePane(true);
            message.setText("Unable to connect");
        });
    }

    private StringProperty statusMessagesProperty() {
        if (statusMessagesProperty == null) {
            statusMessagesProperty = new SimpleStringProperty();
        }
        return statusMessagesProperty;
    }
}

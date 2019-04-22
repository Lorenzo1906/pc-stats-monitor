package com.mythicalcreaturesoftware.pcstatsmonitorclient.controllers;

import com.mythicalcreaturesoftware.pcstatsmonitorclient.services.DataService;
import com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.DataUtils;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Utils;
import eu.hansolo.medusa.Gauge;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class);

    @FXML
    private Label cpuName;

    @FXML
    private Label gpuName;

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
                    LOG.debug("Updating UI data");

                    JSONObject json = new JSONObject(newValue.toString());

                    String cpuNameData = json.has(Keys.CPU_NAME) ? json.getString(Keys.CPU_NAME) : "";
                    String cpuUsageData = json.has(Keys.CPU_USAGE) ? json.getString(Keys.CPU_USAGE) : "";
                    String cpuTempData = json.has(Keys.CPU_TEMP) ? json.getString(Keys.CPU_TEMP) : "";
                    String gpuNameData = json.has(Keys.GPU_NAME) ? json.getString(Keys.GPU_NAME) : "";
                    String gpuUsageData = json.has(Keys.GPU_USAGE) ? json.getString(Keys.GPU_USAGE) : "";
                    String gpuTempData = json.has(Keys.GPU_TEMP) ? json.getString(Keys.GPU_TEMP) : "";
                    String gpuShutdown = json.has(Keys.GPU_SHUTDOWN) ? json.getString(Keys.GPU_SHUTDOWN) : "";
                    String diskTotalData = json.has(Keys.DISK_TOTAL) ? json.getString(Keys.DISK_TOTAL) : "";
                    String diskUsedData = json.has(Keys.DISK_USED) ? json.getString(Keys.DISK_USED) : "";
                    String memTotalData = json.has(Keys.MEM_TOTAL) ? json.getString(Keys.MEM_TOTAL) : "";
                    String memAvailableData = json.has(Keys.MEM_AVAILABLE) ? json.getString(Keys.MEM_AVAILABLE) : "";

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
        );

        dataService.start();
    }

    private void setUpService () {
        LOG.info("Starting data service...");

        dataService = new DataService();
        dataService.setOnSucceeded((WorkerStateEvent t) -> LOG.debug("setOnRunning"));
        dataService.setOnRunning((WorkerStateEvent t) -> LOG.debug("setOnRunning"));
        dataService.setOnFailed((WorkerStateEvent t) -> LOG.debug("setOnFailed"));
    }

    private StringProperty statusMessagesProperty() {
        if (statusMessagesProperty == null) {
            statusMessagesProperty = new SimpleStringProperty();
        }
        return statusMessagesProperty;
    }
}

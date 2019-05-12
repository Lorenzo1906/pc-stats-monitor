package com.mythicalcreaturesoftware.pcstatsmonitorclient.controllers;

import com.mythicalcreaturesoftware.pcstatsmonitorclient.services.DataService;
import com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.ClientKeys;
import com.mythicalcreaturesoftware.pcstatsmonitorclient.utils.DataUtils;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import eu.hansolo.medusa.Gauge;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class);

    @FXML
    private VBox mainContent;

    @FXML
    private BorderPane messageContent;

    @FXML
    private BorderPane audioVisualizer;

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

    @FXML
    private BarChart audioVisualizerChart;

    @FXML
    private NumberAxis numberAxis;

    private DataService dataService;
    private List<XYChart.Data<String, Number>> seriesData;

    public void initialize() {
        setUpService();
        initializeAudio();
        initializeAudioVisualizer();

        dataService.messageProperty().addListener(
            (ObservableValue<?> observable, Object oldValue, Object newValue)-> {

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

    private void initializeAudio() {
        File file = new File("/home/lorenzo/Downloads/file_example_WAV_5MG.wav");

        Media media = new Media(file.toURI().toString());
        MediaPlayer audioMediaPlayer=new MediaPlayer(media);
        audioMediaPlayer.play();

        audioMediaPlayer.setAudioSpectrumListener((double d, double d1, float[] magnitudes , float[] phases) -> {

            for(int i=0; i<magnitudes.length; i++){

                seriesData.get(i).setYValue((magnitudes[i]+60));
            }

        });
    }

    private void initializeAudioVisualizer() {
        numberAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(numberAxis,null,"dB"));

        int maximumSize = 128;
        XYChart.Series<String,Number> series1 = new XYChart.Series<>();
        series1.setName("Series Neg");
        seriesData = new ArrayList<>();

        for (int i=0; i<maximumSize; i++) {
            seriesData.add(i, new XYChart.Data<>( Integer.toString(i+1),50));
            series1.getData().add(seriesData.get(i));
        }

        audioVisualizerChart.getData().add(series1);
    }

    private void showMessagePane (boolean value) {
        mainContent.setVisible(!value);
        audioVisualizer.setVisible(!value);
        messageContent.setVisible(value);
    }

    private void processUIData (String data) {
        if (!DataUtils.isJSONValid(data)) {
            return;
        }

        JSONObject json = new JSONObject(data);

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
}

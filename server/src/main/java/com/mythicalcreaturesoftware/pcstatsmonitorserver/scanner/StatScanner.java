package com.mythicalcreaturesoftware.pcstatsmonitorserver.scanner;

import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Keys;
import com.mythicalcreaturesoftware.pcstatsmonitorserver.util.Utils;
import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Components;
import com.profesorfalken.jsensors.model.components.Gpu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperic.sigar.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class StatScanner {

    private static final Logger LOG = LogManager.getLogger(StatScanner.class);

    private static StatScanner SINGLE_INSTANCE = null;
    private static String TEMP_SCRIPT_PATH;

    private StatScanner() {
        LOG.info("OS detected : " + Utils.getOsName());

        String libPathProperty = System.getProperty("java.library.path");
        LOG.info("\"java.library.path\" set to " + libPathProperty);

        TEMP_SCRIPT_PATH = generateTempFile();
    }

    public Map<String, String> getStatsInfo () {
        Map<String, String> results;

        switch (Utils.getOsValue(Utils.getOsName())) {
            case 0:
                results = getWindowsInfo();
                break;
            case 1:
                throw new UnsupportedOperationException("Mac not supported");
            case 2:
                results = getLinuxInfo();
                break;
            default:
                throw new UnsupportedOperationException("OS not supported");
        }

        return results;
    }

    private Map<String, String> getWindowsInfo () {
        LOG.debug("Getting windows info");

        Map<String, String> results = new HashMap<>();
        Components components = JSensors.get.components();

        results.put(Keys.GPU_NAME, getGpuName(components));
        results.put(Keys.GPU_USAGE, getGpuLoad(components));
        results.put(Keys.GPU_TEMP, getGpuTemp(components));
        results.put(Keys.GPU_SHUTDOWN, Keys.GPU_SHUTDOWN_DEFAULT);//In windows the default is set to 90
        results.put(Keys.CPU_TEMP, getCpuTemp(components));
        results.putAll(getSharedInformation());

        return results;
    }

    private Map<String, String> getLinuxInfo () {
        LOG.debug("Getting linux info");

        Map<String, String> results = new HashMap<>();
        Components components = JSensors.get.components();

        results.putAll(executeBashScript());
        results.putAll(getSharedInformation());
        results.put(Keys.CPU_TEMP, getCpuTemp(components));

        return results;
    }

    private Map<String, String> getSharedInformation() {
        Map<String, String> results = new HashMap<>();
        Sigar sigar = new Sigar();

        try {
            results.put(Keys.CPU_NAME, getCpuName(sigar));
            results.put(Keys.CPU_USAGE, String.valueOf(getCpuUsage(sigar)));
            results.put(Keys.DISK_TOTAL, String.valueOf(getDiskTotal(sigar)));
            results.put(Keys.DISK_USED, String.valueOf(getDiskUsed(sigar)));
            results.put(Keys.MEM_TOTAL, String.valueOf(sigar.getMem().getTotal()));
            results.put(Keys.MEM_AVAILABLE, String.valueOf(sigar.getMem().getActualFree()));
        } catch (SigarException e) {
            LOG.error(e.getMessage());
        }

        return results;
    }

    private Map<String, String> executeBashScript() {
        LOG.debug("Executing bash script");

        Map<String, String> results = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(TEMP_SCRIPT_PATH);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                processLine(line, results);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return results;
    }

    private String getCpuTemp(Components components) {
        LOG.debug("Getting cpu temp");

        double cpuTemp = 0d;

        List<com.profesorfalken.jsensors.model.components.Cpu> cpus = components.cpus;
        if (cpus != null) {
            for (final com.profesorfalken.jsensors.model.components.Cpu cpu : cpus) {
                if (cpu.sensors != null) {
                    List<Double> tempValues = cpu.sensors.temperatures.stream()
                            .map(temp -> temp.value)
                            .collect(Collectors.toList());
                    cpuTemp = Utils.average(tempValues);
                }
            }
        }

        return String.valueOf(cpuTemp);
    }

    private String getGpuTemp(Components components) {
        LOG.debug("Getting gpu temp");

        double gpuTemp = 0d;

        List<Gpu> gpus = components.gpus;
        if (gpus != null) {
            for (final Gpu gpu : gpus) {
                if (gpu.sensors != null) {
                    List<Double> tempValues = gpu.sensors.temperatures.stream()
                            .map(temp -> temp.value)
                            .collect(Collectors.toList());
                    gpuTemp = Utils.average(tempValues);
                }
            }
        }

        return String.valueOf(gpuTemp);
    }

    private String getGpuLoad(Components components) {
        LOG.debug("Getting gpu load");

        double gpuLoad = 0d;

        List<Gpu> gpus = components.gpus;
        if (gpus != null) {
            for (final Gpu gpu : gpus) {
                if (gpu.sensors != null) {
                    List<Double> loadValues = gpu.sensors.loads.stream()
                            .map(temp -> temp.value)
                            .collect(Collectors.toList());
                    gpuLoad = Utils.average(loadValues);
                }
            }
        }

        return String.valueOf(gpuLoad);
    }

    private String getGpuName(Components components) {
        LOG.debug("Getting gpu name");

        List<Gpu> gpus = components.gpus;
        if (gpus != null) {
            for (final Gpu gpu : gpus) {
                if (gpu.sensors != null) {
                    return gpu.name;
                }
            }
        }

        return "";
    }

    private String getCpuName (Sigar sigar) throws SigarException {
        LOG.debug("Getting cpu name");

        String result = "";
        CpuInfo[] infos;

        infos = sigar.getCpuInfoList();
        for (CpuInfo info : infos) {
            result = info.getVendor() + " " + info.getModel();
        }

        return result;
    }

    private double getCpuUsage  (Sigar sigar) throws SigarException {
        LOG.debug("Getting cpu usage");

        CpuPerc perc = sigar.getCpuPerc();
        return perc.getCombined() * 100;
    }

    private double getDiskTotal  (Sigar sigar) throws SigarException {
        LOG.debug("Getting disk total");

        double total = 0d;

        for (String fileSystem: getLocalFileSystemDirectoryNames(sigar)) {

            if (!fileSystem.contains("boot")) {
                FileSystemUsage usageStats = sigar.getFileSystemUsage(fileSystem);
                total += usageStats.getTotal();
            }
        }

        return total;
    }

    private double getDiskUsed  (Sigar sigar) throws SigarException {
        LOG.debug("Getting disk total");

        double used = 0d;

        for (String fileSystem: getLocalFileSystemDirectoryNames(sigar)) {

            if (!fileSystem.contains("boot")) {
                FileSystemUsage usageStats = sigar.getFileSystemUsage(fileSystem);
                used += usageStats.getUsed();
            }
        }

        return used;
    }

    private Set<String> getLocalFileSystemDirectoryNames(Sigar sigar) {
        Set<String> ret = new HashSet<>();
        try {
            FileSystem[] fileSystemList = sigar.getFileSystemList();

            for (FileSystem fs : fileSystemList) {
                if ((fs.getType() == FileSystem.TYPE_LOCAL_DISK)) {
                    ret.add(fs.getDirName());
                }
            }
        }
        catch (SigarException e) {
            LOG.error(e.getMessage());
        }

        return ret;
    }

    private void processLine(String line, Map<String, String> results) {
        StringTokenizer tokenizer = new StringTokenizer(line, Keys.DELIMITER);
        String key = "";
        String value = "";
        boolean first = true;

        while (tokenizer.hasMoreTokens()) {
            if (first) {
                key = tokenizer.nextToken().trim();
                first = false;
            } else {
                value = tokenizer.nextToken().trim();
                if (key.equals(Keys.CPU_TEMP)) {
                    value = value.replace(Keys.CPU_TEMP_PREFIX, "").trim();
                }
            }
        }

        results.put(key, value);
    }

    private String generateTempFile () {
        String path = "";
        try {
            File tempFile = File.createTempFile("tempScript", ".sh");
            tempFile.deleteOnExit();
            tempFile.setExecutable(true);
            path = tempFile.getAbsolutePath();


            InputStream in = StatScanner.class.getClassLoader().getResourceAsStream("system-info.sh");
            byte[] buffer = new byte[in.available()];
            in.read(buffer);

            OutputStream out = new FileOutputStream(tempFile);
            out.write(buffer);
            out.close();

            LOG.debug("Script temp path: " + tempFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return path;
    }

    public static StatScanner getInstance() {
        if (SINGLE_INSTANCE == null) {
            synchronized(StatScanner.class) {
                SINGLE_INSTANCE = new StatScanner();
            }
        }
        return SINGLE_INSTANCE;
    }
}
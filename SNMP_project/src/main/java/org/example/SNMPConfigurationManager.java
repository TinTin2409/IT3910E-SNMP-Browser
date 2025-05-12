// ConfigurationManager.java
package org.example;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;
import org.example.UIHelper;

public class SNMPConfigurationManager {
    private Properties appConfig;
    private List<String> recentTargets;
    private final Preferences userPrefs;
    private static final String CONFIG_FILE = "snmpBrowser.properties";
    private static final String DEFAULT_MIB_PATH = "mibs";
    private static final int MAX_RECENT_TARGETS = 10;

    public SNMPConfigurationManager() {
        this.appConfig = new Properties();
        this.recentTargets = new ArrayList<>();
        this.userPrefs = Preferences.userNodeForPackage(SNMPConfigurationManager.class);
        loadConfiguration();
    }

    private void loadConfiguration() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            appConfig.load(input);

            // Load recent targets from preferences
            String savedTargets = userPrefs.get("recentTargets", "");
            if (!savedTargets.isEmpty()) {
                recentTargets = new ArrayList<>(Arrays.asList(savedTargets.split(",")));
            }
        } catch (IOException e) {
            UIHelper.showError("Configuration Error", "Failed to load configuration: " + e.getMessage());
            // Config file doesn't exist - use defaults
            appConfig.setProperty("mibDirectory", DEFAULT_MIB_PATH);
            appConfig.setProperty("defaultCommunity", "public");
            appConfig.setProperty("defaultPort", "161");
            saveConfiguration();
        }
    }

    public void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            appConfig.store(output, "SNMP Browser Configuration");
            userPrefs.put("recentTargets", String.join(",", recentTargets));
        } catch (IOException e) {
            UIHelper.showError("Configuration Error", "Failed to save configuration: " + e.getMessage());
        }
    }

    public void addRecentTarget(String target) {
        recentTargets.remove(target);
        recentTargets.add(0, target);

        if (recentTargets.size() > MAX_RECENT_TARGETS) {
            recentTargets.remove(recentTargets.size() - 1);
        }

        saveConfiguration();
    }

    public String getDefaultCommunity() {
        return appConfig.getProperty("defaultCommunity", "public");
    }

    public void setDefaultCommunity(String community) {
        appConfig.setProperty("defaultCommunity", community);
        saveConfiguration();
    }

    public int getDefaultPort() {
        return Integer.parseInt(appConfig.getProperty("defaultPort", "161"));
    }

    public void setDefaultPort(int port) {
        appConfig.setProperty("defaultPort", String.valueOf(port));
        saveConfiguration();
    }

    public String getMibDirectory() {
        return appConfig.getProperty("mibDirectory", DEFAULT_MIB_PATH);
    }

    public void setMibDirectory(String path) {
        appConfig.setProperty("mibDirectory", path);
        saveConfiguration();
    }

    public List<String> getRecentTargets() {
        return Collections.unmodifiableList(recentTargets);
    }
}

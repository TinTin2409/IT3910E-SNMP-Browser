// MibManager.java
package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MIBManager {
    private final MIBLoader mibLoader;
    private String currentMibDirectory;
    private final SNMPConfigurationManager configManager;

    public MIBManager(SNMPConfigurationManager configManager) {
        this.mibLoader = new MIBLoader();
        this.configManager = configManager;
        this.currentMibDirectory = configManager.getMibDirectory();
        loadMibFiles();
    }

    public void loadMibFiles() {
        try {
            mibLoader.loadMibsFromFolder(currentMibDirectory);
        } catch (Exception e) {
            UIHelper.showError("Failed to load MIB files", e.getMessage());
        }
    }

    public void importMibFile(File selectedFile) throws IOException {
        Path source = selectedFile.toPath();
        Path target = Paths.get(currentMibDirectory, selectedFile.getName());
        Files.copy(source, target);
        loadMibFiles();
    }

    public void setMibDirectory(String directory) {
        currentMibDirectory = directory;
        configManager.setMibDirectory(directory);
        loadMibFiles();
    }

    public Node lookupNode(String oid) {
        // Fixed: Use lookupNode with String parameter as defined in MIBLoader
        return mibLoader.lookupNode(oid);
    }

    public String getCurrentMibDirectory() {
        return currentMibDirectory;
    }

    public List<String> searchMibNodes(String searchTerm) {
        // Implementing a basic search since MIBLoader doesn't have searchNodes method
        List<String> results = new ArrayList<>();

        // Search through predefined OIDs for matches in name or OID
        for (Map.Entry<String, List<String>> entry : mibLoader.getPredefinedRootOids().entrySet()) {
            String rootOid = entry.getKey();

            // Check if OID contains search term
            if (rootOid.contains(searchTerm)) {
                results.add(rootOid);
            }

            // Check if any MIB filename contains search term
            for (String mibFile : entry.getValue()) {
                if (mibFile.toLowerCase().contains(searchTerm.toLowerCase())) {
                    results.add(rootOid + " (" + mibFile + ")");
                }
            }
        }

        return results;
    }
}
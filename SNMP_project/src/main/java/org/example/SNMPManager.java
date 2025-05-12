// SNMPManager.java
package org.example;

import org.snmp4j.smi.VariableBinding;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.example.UIHelper;
import org.example.Node;
import java.util.ArrayList;
import java.util.Map;
import org.example.SNMPCreateTree;

public class SNMPManager {
    private final MIBManager mibManager;
    private final SNMPOperations snmpOperations;
    private final SNMPDataManager dataManager;
    private final SNMPConfigurationManager configManager;
    private SNMPCreateTree oidTreeBuilder = new SNMPCreateTree();
    private Node oidTreeRoot = null;

    public SNMPManager() {
        this.configManager = new SNMPConfigurationManager();
        this.mibManager = new MIBManager(configManager);
        this.dataManager = new SNMPDataManager(mibManager);
        this.snmpOperations = new SNMPOperations(mibManager, dataManager, configManager);
    }

    // MIB File Operations delegations
    public void loadMibFiles() {
        mibManager.loadMibFiles();
        // Build OID tree from all MIB files in the directory
        try {
            List<String> mibFilePaths = new ArrayList<>();
            File dir = new File(mibManager.getCurrentMibDirectory());
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        mibFilePaths.add(file.getAbsolutePath());
                    }
                }
            }
            oidTreeBuilder.buildTreeFromMultipleMIBs(mibFilePaths);
            oidTreeRoot = oidTreeBuilder.getRoot();
        } catch (Exception e) {
            UIHelper.showError("OID Tree Build Failed", e.getMessage());
        }
    }

    public void importMibFile() {
        File selectedFile = UIHelper.chooseMibFile();
        if (selectedFile != null) {
            try {
                mibManager.importMibFile(selectedFile);
                UIHelper.showInfo("Import Successful", "MIB file imported successfully.");
            } catch (IOException e) {
                UIHelper.showError("Import Failed", e.getMessage());
            }
        }
    }

    public void setMibDirectory() {
        String directory = UIHelper.chooseDirectory();
        if (directory != null) {
            mibManager.setMibDirectory(directory);
        }
    }

    // SNMP Operations delegations
    public String get(String ipAddress, int port, String community, String oid) {
        return snmpOperations.get(ipAddress, port, community, oid);
    }

    public List<String> getNext(String ipAddress, int port, String community, String oid) {
        return snmpOperations.getNext(ipAddress, port, community, oid);
    }

    public List<String> walk(String ipAddress, int port, String community, String oid) {
        return snmpOperations.walk(ipAddress, port, community, oid);
    }

    // Data Management delegations
    public void saveQueryResultsToCSV() {
        String filePath = UIHelper.chooseSaveLocation("csv");
        if (filePath != null) {
            try {
                dataManager.saveResultsToCSV(filePath);
                UIHelper.showInfo("Export Successful", "Query results saved to CSV file.");
            } catch (IOException e) {
                UIHelper.showError("Export Failed", e.getMessage());
            }
        }
    }

    public void selectNode(String oid) {
        dataManager.selectNode(oid);
    }

    public void deselectNode(String oid) {
        dataManager.deselectNode(oid);
    }

    public List<String> searchMibNodes(String searchTerm) {
        return mibManager.searchMibNodes(searchTerm);
    }

    // Configuration Management delegations
    public String getDefaultCommunity() {
        return configManager.getDefaultCommunity();
    }

    public void setDefaultCommunity(String community) {
        configManager.setDefaultCommunity(community);
    }

    public int getDefaultPort() {
        return configManager.getDefaultPort();
    }

    public void setDefaultPort(int port) {
        configManager.setDefaultPort(port);
    }

    public List<String> getRecentTargets() {
        return configManager.getRecentTargets();
    }

    public Node lookupNode(String oid) {
        try {
            return mibManager.lookupNode(oid);
        } catch (Exception e) {
            UIHelper.showError("Lookup Failed", "Failed to find node for OID: " + oid);
            return null;
        }
    }

    // Get children OIDs for a given OID (or root if null/empty)
    public List<Node> getChildrenOfOid(String oid) {
        Node current = oidTreeRoot;
        if (oid != null && !oid.isEmpty()) {
            String[] parts = oid.split("\\.");
            for (String part : parts) {
                if (current == null) break;
                current = current.children.get(part);
            }
        }
        if (current == null) return new ArrayList<>();
        return new ArrayList<>(current.children.values());
    }
}

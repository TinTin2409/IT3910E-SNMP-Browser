// DataManager.java
package org.example;

import org.snmp4j.smi.VariableBinding;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SNMPDataManager {
    private final Map<String, List<VariableBinding>> queryResults;
    private final List<String> selectedNodes;
    private final MIBManager mibManager;

    public SNMPDataManager(MIBManager mibManager) {
        this.queryResults = new HashMap<>();
        this.selectedNodes = new ArrayList<>();
        this.mibManager = mibManager;
    }

    public void storeQueryResult(String oid, VariableBinding vb) {
        if (!queryResults.containsKey(oid)) {
            queryResults.put(oid, new ArrayList<>());
        }
        queryResults.get(oid).add(vb);
    }

    public void saveResultsToCSV(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write header
            writer.println("OID,Value,Type,Description");

            // Write data
            for (Map.Entry<String, List<VariableBinding>> entry : queryResults.entrySet()) {
                for (VariableBinding vb : entry.getValue()) {
                    Node node = mibManager.lookupNode(entry.getKey());
                    String description = (node != null) ? node.description : "";
                    String type = (node != null) ? node.type : "";

                    writer.printf("%s,%s,%s,\"%s\"%n",
                            vb.getOid().toString(),
                            vb.toValueString(),
                            type,
                            description.replace("\"", "\"\"") // Escape quotes for CSV
                    );
                }
            }
        }
    }

    public void selectNode(String oid) {
        if (!selectedNodes.contains(oid)) {
            selectedNodes.add(oid);
        }
    }

    public void deselectNode(String oid) {
        selectedNodes.remove(oid);
    }

    public List<String> getSelectedNodes() {
        return new ArrayList<>(selectedNodes);
    }

    public Map<String, List<VariableBinding>> getQueryResults() {
        return new HashMap<>(queryResults);
    }
}
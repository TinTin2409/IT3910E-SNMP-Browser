// SNMPOperations.java
package org.example.core;

import org.example.config.SNMPConfigurationManager;
import org.example.mib.MIBManager;
import org.example.mib.Node;
import org.example.operation.SNMPGet;
import org.example.operation.SNMPGetNext;
import org.example.operation.SNMPWalk;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SNMPOperations {
    private final MIBManager mibManager;
    private final SNMPDataManager dataManager;
    private final SNMPConfigurationManager configManager;

    public SNMPOperations(MIBManager mibManager, SNMPDataManager dataManager, SNMPConfigurationManager configManager) {
        this.mibManager = mibManager;
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    public String get(String ipAddress, int port, String community, String oid) {
        try {
            if (!validateInput(ipAddress, community, oid)) {
                return "Error: Invalid input parameters";
            }

            String formattedOid = formatOid(oid);
            UdpAddress targetAddress = new UdpAddress(ipAddress + "/" + port);
            configManager.addRecentTarget(ipAddress + ":" + port);

            SNMPGet snmpGet = new SNMPGet(targetAddress, community, formattedOid);
            VariableBinding vb = snmpGet.getVariableBinding();

            if (vb == null) {
                return String.format("%s: No response received", formattedOid);
            }

            String result = formatSnmpResponse(vb, oid);
            dataManager.storeQueryResult(oid, vb);
            return result;
        } catch (Exception e) {
            return String.format("%s: Error - %s", oid, e.getMessage());
        }
    }

    public List<String> getNext(String ipAddress, int port, String community, String oid) {
        try {
            if (!validateInput(ipAddress, community, oid)) {
                return Collections.singletonList("Error: Invalid input parameters");
            }

            UdpAddress targetAddress = new UdpAddress(ipAddress + "/" + port);
            SNMPGetNext snmpGetNext = new SNMPGetNext(targetAddress, community, oid);
            VariableBinding vb = snmpGetNext.getVariableBinding();

            if (vb == null) {
                return Collections.singletonList(String.format("%s: No response received", oid));
            }

            String result = formatSnmpResponse(vb, vb.getOid().toString());
            dataManager.storeQueryResult(vb.getOid().toString(), vb);

            List<String> response = new ArrayList<>();
            response.add(result);
            response.add(vb.getOid().toString());
            return response;
        } catch (Exception e) {
            return Collections.singletonList(String.format("%s: Error - %s", oid, e.getMessage()));
        }
    }

    public List<String> walk(String ipAddress, int port, String community, String oid) {
        try {
            if (!validateInput(ipAddress, community, oid)) {
                return Collections.singletonList("Error: Invalid input parameters");
            }

            UdpAddress targetAddress = new UdpAddress(ipAddress + "/" + port);
            SNMPWalk walker = new SNMPWalk(targetAddress, community);
            walker.start();

            List<VariableBinding> results = walker.performSNMPWalk(oid);
            List<String> formattedResults = new ArrayList<>();

            for (VariableBinding vb : results) {
                String result = formatSnmpResponse(vb, vb.getOid().toString());
                formattedResults.add(result);
                dataManager.storeQueryResult(vb.getOid().toString(), vb);
            }

            return formattedResults;
        } catch (Exception e) {
            return Collections.singletonList(String.format("%s: Error - %s", oid, e.getMessage()));
        }
    }

    private boolean validateInput(String ipAddress, String community, String oid) {
        return ipAddress != null && !ipAddress.trim().isEmpty() &&
                community != null && !community.trim().isEmpty() &&
                oid != null && !oid.trim().isEmpty();
    }

    private String formatOid(String oid) {
        if (!oid.endsWith(".0") && !oid.matches(".*\\.[1-9][0-9]*$")) {
            return oid + ".0";
        }
        return oid;
    }

    private String formatSnmpResponse(VariableBinding vb, String requestedOid) {
        Node node = mibManager.lookupNode(requestedOid);
        String formattedValue;

        if (node != null) {
            formattedValue = SNMPResponseFormatter.format(
                    vb.toValueString(),
                    node.type,
                    node.constraints
            );
        } else {
            formattedValue = vb.toValueString();
        }

        return String.format("%s = %s", vb.getOid(), formattedValue);
    }
}
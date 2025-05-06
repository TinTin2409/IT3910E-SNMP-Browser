package org.example;

import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import java.io.IOException;

public class SNMPManager {
    private final MibLoader mibLoader;

    public SNMPManager(MibLoader mibLoader) {
        this.mibLoader = mibLoader;
    }

    private SNMPGet createSNMPGet(UdpAddress targetAddress, String community, String oid) throws IOException {
        return new SNMPGet(targetAddress, community, oid);
    }

    private String formatOid(String oid) {
        if (!oid.endsWith(".0") && !oid.matches(".*\\.[1-9][0-9]*$")) {
            return oid + ".0";
        }
        return oid;
    }

    public String get(String ipAddress, int port, String community, String oid) {
        try {
            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                return "Error: Invalid IP address";
            }
            if (community == null || community.trim().isEmpty()) {
                return "Error: Invalid community string";
            }
            if (oid == null || oid.trim().isEmpty()) {
                return "Error: Invalid OID";
            }
            
            String formattedOid = formatOid(oid);
            UdpAddress targetAddress = new UdpAddress(ipAddress + "/" + port);
            SNMPGet snmpGet = createSNMPGet(targetAddress, community, formattedOid);
            VariableBinding vb = snmpGet.getVariableBinding();
            
            if (vb == null) {
                return String.format("%s: No response received", formattedOid);
            }
            
            if (vb.isException()) {
                return String.format("%s: Error - %s", formattedOid, vb.toString());
            }

            // Look up the Node information from MIB
            Node node = mibLoader.lookupNode(oid);
            String formattedValue;
            
            if (node != null) {
                // Use SNMPResponseFormatter to format the response based on the node's type and constraints
                formattedValue = SNMPResponseFormatter.format(
                    vb.toValueString(),
                    node.getType(),
                    node.getConstraints()
                );
            } else {
                // If no MIB information is available, use the raw value
                formattedValue = vb.toValueString();
            }
            
            return String.format("%s = %s", vb.getOid(), formattedValue);
            
        } catch (RuntimeException e) {
            return String.format("%s: Error - %s", oid, e.getMessage());
        } catch (IOException e) {
            return String.format("%s: Network Error - %s", oid, e.getMessage());
        }
    }
}
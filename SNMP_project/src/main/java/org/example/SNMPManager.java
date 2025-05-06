package org.example;

import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import java.io.IOException;

public class SNMPManager {
    public String get(String ipAddress, int port, String community, String oid) throws IOException {
        UdpAddress targetAddress = new UdpAddress(ipAddress + "/" + port);
        SNMPGet snmpGet = new SNMPGet(targetAddress, community, oid);
        VariableBinding vb = snmpGet.getVariableBinding();
        
        if (vb != null) {
            return String.format("%s = %s", oid, vb.toValueString());
        } else {
            throw new RuntimeException("SNMP request failed: No response received");
        }
    }
}
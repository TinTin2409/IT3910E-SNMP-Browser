package org.example;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SNMPGet {
    private final VariableBinding vb;

    public SNMPGet(UdpAddress targetAddress, String community, String oid) throws IOException {
        TransportMapping<UdpAddress> transport = null;
        Snmp snmp = null;

        try {
            // Create TransportMapping and listen
            transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(community));
            target.setVersion(SnmpConstants.version2c);
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);

            // Create PDU
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            // Create Snmp instance and send request
            snmp = new Snmp(transport);
            ResponseEvent<UdpAddress> response = snmp.send(pdu, target);

            // Process Response
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU.getErrorStatus() == PDU.noError) {
                    vb = responsePDU.get(0);
                } else {
                    throw new IOException("SNMP Error: " + responsePDU.getErrorStatusText());
                }
            } else {
                throw new IOException("Timeout or null response");
            }
        } finally {
            // Cleanup resources
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException e) {
                    // Log or handle cleanup error
                }
            }
            if (transport != null) {
                try {
                    transport.close();
                } catch (IOException e) {
                    // Log or handle cleanup error
                }
            }
        }
    }

    public VariableBinding getVariableBinding() {
        return vb;
    }
}
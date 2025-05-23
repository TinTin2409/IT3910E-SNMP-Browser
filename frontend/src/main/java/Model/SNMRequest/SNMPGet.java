package Model.SNMRequest;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;



public class SNMPGet {

    private final VariableBinding vb; // Variable binding to store the response from the SNMP agent

    private TransportMapping<UdpAddress> createTransportMapping() throws IOException {
        // Create a transport mapping using UDP protocol. This is used by the Snmp class to send requests.
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen(); // Start listening for responses
        return transport;
    }

    private CommunityTarget<UdpAddress> createTarget(UdpAddress ipAddress, String community) {
        // Create a target address. This is where the SNMP request will be sent.
        CommunityTarget<UdpAddress> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community)); // Set the community string
        target.setVersion(SnmpConstants.version2c); // Set the SNMP version. Could be v1, v2c, or v3.
        target.setAddress(ipAddress); // Set the address of the SNMP agent. The port number is 161 for SNMP Get request.
        target.setRetries(2); // Set the number of retries when a request fails.
        target.setTimeout(1000); // Set the timeout (in milliseconds).
        return target;
    }

    private PDU createPDU(String oid) {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid))); // Add an OID (Object Identifier) to the PDU. This is what you want to get from the SNMP agent.
        pdu.setType(PDU.GET); // Set the type of the PDU to GETNEXT. It could also be SET, GET, GETBULK, etc.
        return pdu;
    }

    private VariableBinding processResponse(ResponseEvent<UdpAddress> response) {
        VariableBinding vb = null;
        if (response != null) {
            PDU responsePDU = response.getResponse(); //Retrieve the response PDU

            if (responsePDU != null) {
                int errorStatus = responsePDU.getErrorStatus();

                if (errorStatus == PDU.noError) {
                    vb = responsePDU.get(0); // Get the first VariableBinding directly
                } else {
                    System.out.println("Error: Request Failed");
                }
            } else {
                System.out.println("Error: Response PDU is null");
            }
        } else {
            System.out.println("Error: Agent Timeout...");
        }
        return vb;
    }

    public SNMPGet(UdpAddress ipAddress, String community, String oid) throws IOException {
        // Create a transport mapping
        TransportMapping<UdpAddress> transport = createTransportMapping();

        // Create a target address
        CommunityTarget<UdpAddress> target = createTarget(ipAddress, community);

        // Create a PDU
        PDU pdu = createPDU(oid);

        // Create an SNMP instance
        Snmp snmp = new Snmp(transport);

        // Send the GET request
        ResponseEvent<UdpAddress> response = snmp.send(pdu, target);

        // Process the response
        vb = processResponse(response);

        // Close the SNMP session
        snmp.close();
    }

    public VariableBinding getVariableBinding() {
        return this.vb;
    }

}
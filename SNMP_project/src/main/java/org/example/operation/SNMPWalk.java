package org.example.operation;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
public class SNMPWalk {

    private static final int RETRIES = 1; // Number of retries when a request fails
    private static final int TIMEOUT = 150; // Timeout in milliseconds
    private static final int VERSION = SnmpConstants.version2c; // SNMP version. Could be v1, v2c, or v3

    private Snmp snmp = null;
    private UdpAddress ipAddress = null;
    private String communityString = null;
    public SNMPWalk(UdpAddress ipAddress, String communityString) {
        this.ipAddress = ipAddress;
        this.communityString = communityString;
    }

    public void start() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    private CommunityTarget<UdpAddress> createCommunityTarget() {
        CommunityTarget<UdpAddress> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(communityString));
        target.setAddress(ipAddress);
        target.setRetries(RETRIES);
        target.setTimeout(TIMEOUT);
        target.setVersion(VERSION);
        return target;
    }
    public List<VariableBinding> performSNMPWalk(String startOid) throws IOException {
        //System.out.println("Performing SNMP walk for OID: " + startOid);

        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> events = treeUtils.getSubtree(createCommunityTarget(), new OID(startOid));

        if (events == null || events.isEmpty()) {
            System.out.println("No result returned.");
            return Collections.emptyList();
        }

        //System.out.println("SNMP walk completed. Extracting VariableBindings...");

        List<VariableBinding> varBindings = extractVariableBindingsFromEvents(startOid, events);

        //System.out.println("Extracted " + varBindings.size() + " VariableBindings");

        return varBindings;
    }

    private List<VariableBinding> extractVariableBindingsFromEvents(String startOid, List<TreeEvent> events) {
        List<VariableBinding> varBindingsList = new ArrayList<>();

        for (TreeEvent event : events) {
            if (event != null) {
                if (event.isError()) {
                    System.err.println("oid [" + startOid + "] " + event.getErrorMessage());
                }

                VariableBinding[] varBindings = event.getVariableBindings();
                if (varBindings != null) {
                    //System.out.println("Found " + varBindings.length + " VariableBindings in event");
                    varBindingsList.addAll(Arrays.asList(varBindings));
                }
            }
        }
        return varBindingsList;
    }
}
package frontend;

public class SNMPData {
    private String oid;
    private String value;
    private String type;
    private String ipPort;

    public SNMPData() {
        // Required for JavaFX and JSON deserialization
    }

    public SNMPData(String oid, String value, String type, String ipPort) {
        this.oid = oid;
        this.value = value;
        this.type = type;
        this.ipPort = ipPort;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpPort() {
        return ipPort;
    }

    public void setIpPort(String ipPort) {
        this.ipPort = ipPort;
    }
}

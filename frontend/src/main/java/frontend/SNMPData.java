package frontend;

import javafx.beans.property.SimpleStringProperty;

public class SNMPData {
    private final SimpleStringProperty oid = new SimpleStringProperty();
    private final SimpleStringProperty value = new SimpleStringProperty();
    private final SimpleStringProperty type = new SimpleStringProperty();
    private final SimpleStringProperty ipPort = new SimpleStringProperty();

    public SNMPData() {
        // Required by Jackson
    }

    public SNMPData(String oid, String value, String type, String ipPort) {
        this.oid.set(oid);
        this.value.set(value);
        this.type.set(type);
        this.ipPort.set(ipPort);
    }

    public String getOid() { return oid.get(); }
    public void setOid(String oid) { this.oid.set(oid); }

    public String getValue() { return value.get(); }
    public void setValue(String value) { this.value.set(value); }

    public String getType() { return type.get(); }
    public void setType(String type) { this.type.set(type); }

    public String getIpPort() { return ipPort.get(); }
    public void setIpPort(String ipPort) { this.ipPort.set(ipPort); }
}

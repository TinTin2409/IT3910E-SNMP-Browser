public class SNMPData {
    private final javafx.beans.property.SimpleStringProperty oid;
    private final javafx.beans.property.SimpleStringProperty value;
    private final javafx.beans.property.SimpleStringProperty type;
    private final javafx.beans.property.SimpleStringProperty ipPort;

    public SNMPData(String oid, String value, String type, String ipPort) {
        this.oid = new javafx.beans.property.SimpleStringProperty(oid);
        this.value = new javafx.beans.property.SimpleStringProperty(value);
        this.type = new javafx.beans.property.SimpleStringProperty(type);
        this.ipPort = new javafx.beans.property.SimpleStringProperty(ipPort);
    }

    public String getOid() { return oid.get(); }
    public String getValue() { return value.get(); }
    public String getType() { return type.get(); }
    public String getIpPort() { return ipPort.get(); }
}

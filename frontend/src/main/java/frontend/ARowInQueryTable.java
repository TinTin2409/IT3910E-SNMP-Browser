package frontend;

public class ARowInQueryTable {
    private final String name;
    private final String value;
    private final String type;

    public ARowInQueryTable(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
package frontend;

public class Node {
    public String oid;
    public String name;
    public String type;
    public String description;

    public Node(String oid, String name, String type, String description) {
        this.oid = oid;
        this.name = name;
        this.type = type;
        this.description = description;
    }

    @Override
    public String toString() {
        return name + " (" + oid + ")";
    }
}

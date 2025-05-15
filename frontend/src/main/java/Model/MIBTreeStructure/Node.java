package Model.MIBTreeStructure;

import java.util.HashMap;
import java.util.Map;

public class Node {
    public String name;
    public String oid;
    public String nodeType;
    public String type;
    public String access;
    public String status;
    public String description;

    public Map<String, Object> constraints; // Constraints, include the name of the constraints and the actual constraints, since we do not
    // know in advance the type of the constraints, we use the Object type to store the constraints object.

    Map<String, Node> children = new HashMap<>();

    Node(String name, String oid, String nodeType, String type, String access, String status, String description, Map<String, Object> constraints) {
        this.name = name;
        this.oid = oid;
        this.nodeType = nodeType;
        this.type = type;
        this.access = access;
        this.status = status;
        this.description = description;
        this.constraints = constraints;
    }

    /**
     * When display the TreeView to the UI, each node has the Node type as we defined in this class. We just want
     * to display the Node name, not the whole Node object represents in string format. Override the toString method from TreeItem class
     *
     * @return Node name in the UI
     */
    @Override
    public String toString() {
        return name;
    }
}
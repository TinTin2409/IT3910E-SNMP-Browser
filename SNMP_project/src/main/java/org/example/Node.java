package org.example;

import java.util.HashMap;
import java.util.Map;

public class Node {
    private String name;
    private String oid;
    private String nodeType;
    private String type;
    private String access;
    private String status;
    private String description;
    private Map<String, Object> constraints;
    private Map<String, Node> children;

    public Node(String name, String oid, String nodeType, String type, 
                String access, String status, String description, 
                Map<String, Object> constraints) {
        this.name = name;
        this.oid = oid;
        this.nodeType = nodeType;
        this.type = type;
        this.access = access;
        this.status = status;
        this.description = description;
        this.constraints = constraints != null ? constraints : new HashMap<>();
        this.children = new HashMap<>();
    }

    // Getters
    public String getName() { return name; }
    public String getOid() { return oid; }
    public String getNodeType() { return nodeType; }
    public String getType() { return type; }
    public String getAccess() { return access; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public Map<String, Object> getConstraints() { return constraints; }
    public Map<String, Node> getChildren() { return children; }

    public void addChild(Node child) {
        children.put(child.getName(), child);
    }

    public Node findNodeByOid(String searchOid) {
        if (this.oid.equals(searchOid)) {
            return this;
        }
        for (Node child : children.values()) {
            Node result = child.findNodeByOid(searchOid);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MibLoader {
    private final Map<String, List<JsonNode>> mibFilesByRootOid = new HashMap<>();
    private final Map<String, List<String>> predefinedRootOids;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MibLoader() {
        this.predefinedRootOids = initializePredefinedOids();
    }

    private Map<String, List<String>> initializePredefinedOids() {
        Map<String, List<String>> oids = new HashMap<>();
        // Add your predefined OIDs here
        oids.put("1.3.6.1.2.1.25", Arrays.asList("HOST-RESOURCES-MIB.json"));
        oids.put("1.3.6.1.2.1.1", Arrays.asList("SNMPv2-MIB.json"));
        // Add other predefined OIDs as needed
        return oids;
    }

    public void loadMibsFromFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid MIB folder path: " + folderPath);
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) {
            throw new IllegalArgumentException("Unable to list files in directory: " + folderPath);
        }

        for (File file : files) {
            try {
                JsonNode fileNode = objectMapper.readTree(file);
                String rootOid = getRootOidFromFile(file.getName());
                if (rootOid != null) {
                    mibFilesByRootOid.computeIfAbsent(rootOid, k -> new ArrayList<>()).add(fileNode);
                }
            } catch (IOException e) {
                System.err.println("Error loading MIB file: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private String getRootOidFromFile(String fileName) {
        for (Map.Entry<String, List<String>> entry : predefinedRootOids.entrySet()) {
            if (entry.getValue().contains(fileName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Node lookupNode(String oid) {
        for (Map.Entry<String, List<JsonNode>> entry : mibFilesByRootOid.entrySet()) {
            String rootOid = entry.getKey();
            if (oid.startsWith(rootOid)) {
                for (JsonNode mibFile : entry.getValue()) {
                    Node node = findNodeWithOid(mibFile, oid);
                    if (node != null) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    private Node findNodeWithOid(JsonNode currentNode, String targetOid) {
        if (currentNode == null) {
            return null;
        }

        if (currentNode.has("oid") && targetOid.equals(currentNode.get("oid").asText())) {
            return createNodeFromJson(currentNode);
        }

        if (currentNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = currentNode.fields();
            while (fields.hasNext()) {
                Node node = findNodeWithOid(fields.next().getValue(), targetOid);
                if (node != null) {
                    return node;
                }
            }
        } else if (currentNode.isArray()) {
            for (JsonNode element : currentNode) {
                Node node = findNodeWithOid(element, targetOid);
                if (node != null) {
                    return node;
                }
            }
        }

        return null;
    }

    private Node createNodeFromJson(JsonNode jsonNode) {
        String name = jsonNode.path("name").asText(null);
        String oid = jsonNode.path("oid").asText(null);
        String nodeType = jsonNode.path("nodetype").asText(null);
        String type = null;
        Map<String, Object> constraints = new HashMap<>();

        JsonNode syntaxNode = jsonNode.get("syntax");
        if (syntaxNode != null) {
            type = syntaxNode.path("type").asText(null);
            JsonNode constraintsNode = syntaxNode.get("constraints");
            if (constraintsNode != null) {
                constraints = objectMapper.convertValue(constraintsNode, Map.class);
            }
        }

        String access = jsonNode.has("maxaccess") ? jsonNode.get("maxaccess").asText() : null;
        String status = jsonNode.has("status") ? jsonNode.get("status").asText() : null;
        String description = jsonNode.path("description").asText(null);

        return new Node(name, oid, nodeType, type, access, status, description, constraints);
    }
}
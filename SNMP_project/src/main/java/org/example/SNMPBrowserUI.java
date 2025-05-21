package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SNMPBrowserUI extends JFrame {
    // Main components
    private JTabbedPane tabbedPane;
    private JTextField ipAddressField;
    private JTextField portField;
    private JTextField communityField;
    private JTextField oidField;
    private JTextArea resultArea;
    private JTable queryTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> recentTargetsCombo;
    private JList<String> mibsList;
    private DefaultListModel<String> mibsListModel;
    private JTree walkTree;
    private DefaultTreeModel walkTreeModel;

    // Status components
    private JLabel statusLabel;
    private JProgressBar operationProgress;

    // Core functionality provider
    private SNMPManager snmpManager;

    // Track the current OID path for navigation
    private String currentOidPath = "";
    private JButton backButton;

    private JPanel mainPanel;

    public SNMPBrowserUI() {
        super("SNMP Browser");

        // Initialize the manager
        this.snmpManager = new SNMPManager();

        initializeUI();
        loadInitialData();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Main content panel
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create all panels
        JPanel quickQueryPanel = createQueryPanel();
        JPanel mibBrowserPanel = createMIBBrowserPanel();
        JPanel walkTreePanel = createWalkTreePanel();

        // Center split: MIB Browser (left) and Walk Tree (right)
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mibBrowserPanel, walkTreePanel);
        centerSplit.setDividerLocation(500);
        centerSplit.setResizeWeight(0.5);

        mainPanel.add(quickQueryPanel, BorderLayout.NORTH);
        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // Add status bar
        JPanel statusPanel = createStatusPanel();

        // Add to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createToolBar(), BorderLayout.NORTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(statusPanel, BorderLayout.SOUTH);

        // Add back button listener
        backButton.addActionListener(e -> {
            if (!currentOidPath.isEmpty()) {
                // Go up one level
                int lastDot = currentOidPath.lastIndexOf('.');
                if (lastDot > 0) {
                    currentOidPath = currentOidPath.substring(0, lastDot);
                } else {
                    currentOidPath = "";
                }
                
                // Load children of parent node
                List<org.example.Node> children = snmpManager.getChildrenOfOid(currentOidPath);
                updateMibsListWithChildren(children);
                
                // Update back button state
                backButton.setEnabled(!currentOidPath.isEmpty());
            }
        });
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Target selection/configuration
        recentTargetsCombo = new JComboBox<>();
        recentTargetsCombo.setEditable(true);

        ipAddressField = new JTextField("127.0.0.1", 20);
        portField = new JTextField("161", 5);
        communityField = new JTextField(snmpManager.getDefaultCommunity(), 10);
        oidField = new JTextField("1.3.6.1.2.1.1.1.0", 30);

        // Layout components
        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetPanel.add(new JLabel("Recent:"));
        targetPanel.add(recentTargetsCombo);

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.add(new JLabel("IP:"));
        connectionPanel.add(ipAddressField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(new JLabel("Community String:"));
        connectionPanel.add(communityField);

        JPanel oidPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        oidPanel.add(new JLabel("OID:"));
        oidPanel.add(oidField);

        // Add components to form
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(targetPanel, gbc);

        gbc.gridy = 1;
        formPanel.add(connectionPanel, gbc);

        gbc.gridy = 2;
        formPanel.add(oidPanel, gbc);

        // Create buttons
        JPanel buttonPanel = new JPanel();
        JButton getButton = new JButton("Scalar Get");
        JButton getNextButton = new JButton("Get Next");
        JButton walkButton = new JButton("Walk");
        JButton smartQueryButton = new JButton("Tabular Get");

        buttonPanel.add(getButton);
        buttonPanel.add(getNextButton);
        buttonPanel.add(walkButton);
        buttonPanel.add(smartQueryButton);

        gbc.gridy = 3;
        formPanel.add(buttonPanel, gbc);

        // Add action listeners
        getButton.addActionListener(e -> performGet());
        getNextButton.addActionListener(e -> performGetNext());
        walkButton.addActionListener(e -> performWalk());
        smartQueryButton.addActionListener(e -> performSmartQuery());
        recentTargetsCombo.addActionListener(e -> handleRecentTargetSelection());

        panel.add(formPanel, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createMIBBrowserPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create search panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");

        // Add ActionListener to searchField to handle Enter key
        searchField.addActionListener(e -> searchMibs(searchField.getText()));

        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchInputPanel.add(new JLabel("Search:"));
        searchInputPanel.add(searchField);
        searchInputPanel.add(searchButton);

        // Add Back button
        backButton = new JButton("Back");
        backButton.setEnabled(false);
        searchInputPanel.add(backButton);

        // Add Refresh MIBs button
        JButton refreshMibsButton = new JButton("Refresh MIBs");
        searchInputPanel.add(refreshMibsButton);
        refreshMibsButton.addActionListener(e -> {
            snmpManager.loadMibFiles();
            loadMibsIntoList();
            statusLabel.setText("MIBs refreshed");
        });

        searchPanel.add(searchInputPanel, BorderLayout.NORTH);

        // Create MIBs list
        mibsListModel = new DefaultListModel<>();
        mibsList = new JList<>(mibsListModel);
        mibsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane mibsScroll = new JScrollPane(mibsList);
        mibsScroll.setBorder(BorderFactory.createTitledBorder("MIBs and OIDs"));
        mibsScroll.setPreferredSize(new Dimension(400, 600)); // Set preferred size

        // Create detail panel
        JPanel detailPanel = new JPanel(new BorderLayout());
        JTextArea nodeDetailArea = new JTextArea();
        nodeDetailArea.setEditable(false);
        JScrollPane detailScroll = new JScrollPane(nodeDetailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Node Details"));
        detailScroll.setPreferredSize(new Dimension(400, 600)); // Set preferred size

        // Action buttons
        JPanel mibActionsPanel = new JPanel();
        JButton importMibButton = new JButton("Import MIB");
        JButton importMibDirButton = new JButton("Import MIB Directory");
        JButton querySelectedButton = new JButton("Query Selected");

        mibActionsPanel.add(importMibButton);
        mibActionsPanel.add(importMibDirButton);
        mibActionsPanel.add(querySelectedButton);

        detailPanel.add(detailScroll, BorderLayout.CENTER);
        detailPanel.add(mibActionsPanel, BorderLayout.SOUTH);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, mibsScroll, detailPanel);
        splitPane.setDividerLocation(400); // Set initial divider location
        splitPane.setResizeWeight(0.5); // Make both sides equal

        // Add listeners
        searchButton.addActionListener(e -> searchMibs(searchField.getText()));
        importMibButton.addActionListener(e -> importMib());
        importMibDirButton.addActionListener(e -> importMibDirectory());
        querySelectedButton.addActionListener(e -> querySelectedNode());
        
        // Add selection listener to show details
        mibsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedNode = mibsList.getSelectedValue();
                if (selectedNode != null) {
                    displayNodeDetails(selectedNode, nodeDetailArea);
                }
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createWalkTreePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create tree model and tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("SNMP Tree");
        walkTreeModel = new DefaultTreeModel(root);
        walkTree = new JTree(walkTreeModel);
        walkTree.setShowsRootHandles(true);
        walkTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Add tree to scroll pane
        JScrollPane treeScroll = new JScrollPane(walkTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("SNMP Query Results"));

        // Add double-click listener to show details
        walkTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            walkTree.getLastSelectedPathComponent();
                    if (node != null && node.isLeaf()) {
                        showNodeDetails(node.getUserObject().toString());
                    }
                }
            }
        });

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) walkTree.getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        panel.add(treeScroll, BorderLayout.CENTER);
        return panel;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton aboutButton = new JButton("About");

        toolBar.add(aboutButton);

        aboutButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "SNMP Browser v1.0\nDeveloped by TinTin",
                "About SNMP Browser",
                JOptionPane.INFORMATION_MESSAGE
        ));

        return toolBar;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("Ready");
        operationProgress = new JProgressBar();
        operationProgress.setStringPainted(true);
        operationProgress.setString("");
        operationProgress.setVisible(false);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(operationProgress, BorderLayout.EAST);

        return panel;
    }

    private void loadInitialData() {
        // Load MIBs and build OID tree first!
        snmpManager.loadMibFiles();

        // Load recent targets into combobox
        for (String target : snmpManager.getRecentTargets()) {
            recentTargetsCombo.addItem(target);
        }

        // Now load the root OIDs into the list
        loadMibsIntoList();
    }

    private void loadMibsIntoList() {
        mibsListModel.clear();
        currentOidPath = "";
        java.util.List<org.example.Node> rootChildren = snmpManager.getChildrenOfOid("");
        updateMibsListWithChildren(rootChildren);
        backButton.setEnabled(false);
    }

    private void updateMibsListWithChildren(java.util.List<org.example.Node> children) {
        mibsListModel.clear();
        for (org.example.Node child : children) {
            if (child.oid != null) {
                String displayText = child.oid;
                if (child.name != null) {
                    displayText += " (" + child.name + ")";
                }
                mibsListModel.addElement(displayText);
                // Debug output
                System.out.println("Added to list: " + displayText);
            }
        }
    }

    private void performGet() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            setInProgress("Performing Get operation...");

            String result = snmpManager.get(ipAddress, port, community, oid);
            String[] parts = result.split(" = ");
            if (parts.length >= 2) {
                displaySingleOidInTree(parts[0], parts[1].trim());
                // Expand the tree to the queried OID
                expandTreeToOid(oid);
            }

            setCompleted("Get operation completed");
        } catch (NumberFormatException e) {
            UIHelper.showError("Error", "Invalid port number");
            setCompleted("Error: Invalid port number");
        } catch (Exception e) {
            UIHelper.showError("Error", e.getMessage());
            setCompleted("Error: " + e.getMessage());
        }
    }

    private void performGetNext() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            setInProgress("Performing GetNext operation...");

            List<String> result = snmpManager.getNext(ipAddress, port, community, oid);
            if (result.size() > 0) {
                String[] parts = result.get(0).split(" = ");
                if (parts.length >= 2) {
                    displaySingleOidInTree(parts[0], parts[1].trim());
                    // Expand the tree to the next OID
                    expandTreeToOid(parts[0]);
                }
            }

            setCompleted("GetNext operation completed");
        } catch (NumberFormatException e) {
            UIHelper.showError("Error", "Invalid port number");
            setCompleted("Error: Invalid port number");
        } catch (Exception e) {
            UIHelper.showError("Error", e.getMessage());
            setCompleted("Error: " + e.getMessage());
        }
    }

    private void expandTreeToOid(String targetOid) {
        String[] oidParts = targetOid.split("\\.");
        StringBuilder pathBuilder = new StringBuilder();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) walkTreeModel.getRoot();
        
        // First expand the root
        walkTree.expandRow(0);
        
        // Then expand each part of the path
        for (int i = 0; i < oidParts.length; i++) {
            if (i > 0) pathBuilder.append(".");
            pathBuilder.append(oidParts[i]);
            String currentPath = pathBuilder.toString();
            
            // Find the node in the tree
            for (int j = 0; j < root.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(j);
                if (child.getUserObject().equals(currentPath)) {
                    // Expand this node
                    int row = walkTree.getRowForPath(new TreePath(child.getPath()));
                    if (row != -1) {
                        walkTree.expandRow(row);
                    }
                    root = child;
                    break;
                }
            }
        }
    }

    private void performWalk() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            setInProgress("Performing Walk operation...");

            List<String> results = snmpManager.walk(ipAddress, port, community, oid);

            // Build the tree
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) walkTreeModel.getRoot();
            root.removeAllChildren();
            walkTreeModel.reload();

            Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();
            nodeMap.put("", root);

            for (String result : results) {
                String[] parts = result.split(" = ");
                if (parts.length < 2) continue;
                String resultOid = parts[0];
                String value = parts[1].trim();

                String[] oidParts = resultOid.split("\\.");
                StringBuilder pathBuilder = new StringBuilder();
                DefaultMutableTreeNode parent = root;

                for (int i = 0; i < oidParts.length; i++) {
                    if (i > 0) pathBuilder.append(".");
                    pathBuilder.append(oidParts[i]);
                    String path = pathBuilder.toString();

                    DefaultMutableTreeNode node = nodeMap.get(path);
                    if (node == null) {
                        node = new DefaultMutableTreeNode(path);
                        parent.add(node);
                        nodeMap.put(path, node);
                    }
                    parent = node;
                }
                // Add the value as a leaf node
                DefaultMutableTreeNode valueNode = new DefaultMutableTreeNode(result);
                parent.add(valueNode);
            }
            walkTreeModel.reload();
            
            // Expand the tree to the queried OID
            expandTreeToOid(oid);

            setCompleted("Walk operation completed - " + results.size() + " results");
        } catch (NumberFormatException e) {
            UIHelper.showError("Error", "Invalid port number");
            setCompleted("Error: Invalid port number");
        } catch (Exception e) {
            UIHelper.showError("Error", e.getMessage());
            setCompleted("Error: " + e.getMessage());
        }
    }

    private void handleRecentTargetSelection() {
        String selected = (String) recentTargetsCombo.getSelectedItem();
        if (selected != null && selected.contains(":")) {
            String[] parts = selected.split(":");
            ipAddressField.setText(parts[0]);
            if (parts.length > 1) {
                portField.setText(parts[1]);
            }
        }
    }

    private void searchMibs(String searchTerm) {
        setInProgress("Searching MIBs...");

        mibsListModel.clear();

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // If search is empty, show all predefined root OIDs and their MIB files
            java.util.Map<String, java.util.List<String>> rootOids = snmpManager.getPredefinedRootOids();
            for (java.util.Map.Entry<String, java.util.List<String>> entry : rootOids.entrySet()) {
                String rootOid = entry.getKey();
                java.util.List<String> mibFiles = entry.getValue();
                for (String mibFile : mibFiles) {
                    mibsListModel.addElement(rootOid + " (" + mibFile + ")");
                }
            }
            setCompleted("Showing all predefined root OIDs");
            return;
        }

        // Otherwise, filter only among the children of the current node
        List<org.example.Node> children = snmpManager.getChildrenOfOid(currentOidPath);
        int count = 0;
        for (org.example.Node child : children) {
            if ((child.oid != null && child.oid.contains(searchTerm)) ||
                (child.name != null && child.name.toLowerCase().contains(searchTerm.toLowerCase()))) {
                mibsListModel.addElement(child.oid + (child.name != null ? " (" + child.name + ")" : ""));
                count++;
            }
        }

        setCompleted("Search completed - " + count + " results");
    }


    private void displayNodeDetails(String selectedNode, JTextArea detailArea) {
        // Extract OID from selection (assuming format "OID (MIB_NAME)" or just "OID")
        String oid = selectedNode;
        if (selectedNode.contains(" (")) {
            oid = selectedNode.substring(0, selectedNode.indexOf(" ("));
        }

        Node node = snmpManager.lookupNode(oid);
        if (node != null) {
            StringBuilder details = new StringBuilder();
            details.append("Name: ").append(node.name).append("\n");
            details.append("OID: ").append(node.oid).append("\n");
            details.append("Type: ").append(node.type).append("\n");
            details.append("Access: ").append(node.access).append("\n");
            details.append("Status: ").append(node.status).append("\n");
            details.append("Description: ").append(node.description).append("\n");

            detailArea.setText(details.toString());
            snmpManager.selectNode(oid);

            // Check if this is a leaf node (has no children)
            List<org.example.Node> children = snmpManager.getChildrenOfOid(oid);
            if (children.isEmpty()) {
                // This is a leaf node, don't navigate
                return;
            }

            // Not a leaf node, navigate to children
            currentOidPath = oid;
            updateMibsListWithChildren(children);
            
            // Enable back button if we're not at root
            backButton.setEnabled(!currentOidPath.isEmpty());
        } else {
            detailArea.setText("No detailed information available for this node.");
        }
    }

    private void importMib() {
        snmpManager.importMibFile();
        // Refresh the list for the current node
        List<org.example.Node> children = snmpManager.getChildrenOfOid(currentOidPath);
        updateMibsListWithChildren(children);
    }

    private void importMibDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select MIB Directory to Import");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedDir = fileChooser.getSelectedFile().getAbsolutePath();
            snmpManager.importMibDirectory(selectedDir);
            // Refresh the list
            loadMibsIntoList();
            statusLabel.setText("MIB directory imported: " + selectedDir);
        }
    }

    private void querySelectedNode() {
        String selectedNode = mibsList.getSelectedValue();
        if (selectedNode != null) {
            // Extract OID from selection
            String oid = selectedNode;
            if (selectedNode.contains(" (")) {
                oid = selectedNode.substring(0, selectedNode.indexOf(" ("));
            }

            // Set the OID in the query field
            oidField.setText(oid);
            
            // Update status
            statusLabel.setText("Selected OID: " + oid);
            
            // Debug output
            System.out.println("Selected node: " + selectedNode);
            System.out.println("Extracted OID: " + oid);
            System.out.println("Set OID field to: " + oidField.getText());
        } else {
            statusLabel.setText("No node selected");
        }
    }

    private void setInProgress(String message) {
        statusLabel.setText(message);
        operationProgress.setVisible(true);
        operationProgress.setIndeterminate(true);
    }

    private void setCompleted(String message) {
        statusLabel.setText(message);
        operationProgress.setVisible(false);
        operationProgress.setIndeterminate(false);
    }

    private void performSmartQuery() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            setInProgress("Performing Smart Query...");

            // Heuristic: if OID ends with .0, treat as scalar, else treat as table
            if (oid.trim().endsWith(".0")) {
                String result = snmpManager.get(ipAddress, port, community, oid);
                String[] parts = result.split(" = ");
                if (parts.length >= 2) {
                    // For scalar values, create a simpler tree structure
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) walkTreeModel.getRoot();
                    root.removeAllChildren();
                    
                    // Create a single node with the OID and value
                    DefaultMutableTreeNode valueNode = new DefaultMutableTreeNode(result);
                    root.add(valueNode);
                    
                    walkTreeModel.reload();
                    walkTree.expandRow(0);
                }
            } else {
                List<String> results = snmpManager.walk(ipAddress, port, community, oid);
                // Build the tree (same as performWalk)
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) walkTreeModel.getRoot();
                root.removeAllChildren();
                walkTreeModel.reload();

                Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();
                nodeMap.put("", root);

                for (String result : results) {
                    String[] parts = result.split(" = ");
                    if (parts.length < 2) continue;
                    String resultOid = parts[0];
                    String value = parts[1].trim();

                    String[] oidParts = resultOid.split("\\.");
                    StringBuilder pathBuilder = new StringBuilder();
                    DefaultMutableTreeNode parent = root;

                    for (int i = 0; i < oidParts.length; i++) {
                        if (i > 0) pathBuilder.append(".");
                        pathBuilder.append(oidParts[i]);
                        String path = pathBuilder.toString();

                        DefaultMutableTreeNode node = nodeMap.get(path);
                        if (node == null) {
                            node = new DefaultMutableTreeNode(path);
                            parent.add(node);
                            nodeMap.put(path, node);
                        }
                        parent = node;
                    }
                    // Add the value as a leaf node
                    DefaultMutableTreeNode valueNode = new DefaultMutableTreeNode(result);
                    parent.add(valueNode);
                }
                walkTreeModel.reload();
                
                // Expand the tree to the queried OID
                expandTreeToOid(oid);
            }

            setCompleted("Smart Query completed");
        } catch (NumberFormatException e) {
            UIHelper.showError("Error", "Invalid port number");
            setCompleted("Error: Invalid port number");
        } catch (Exception e) {
            UIHelper.showError("Error", e.getMessage());
            setCompleted("Error: " + e.getMessage());
        }
    }

    private void showNodeDetails(String nodeInfo) {
        String[] parts = nodeInfo.split(" = ");
        if (parts.length < 2) return;

        String oid = parts[0];
        String value = parts[1].trim();

        JTextArea textArea = new JTextArea(
            "OID: " + oid + "\n\n" +
            "Value: " + value
        );
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Node Details",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void displaySingleOidInTree(String oid, String value) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) walkTreeModel.getRoot();
        root.removeAllChildren();
        walkTreeModel.reload();

        String[] oidParts = oid.split("\\.");
        StringBuilder pathBuilder = new StringBuilder();
        DefaultMutableTreeNode parent = root;

        for (int i = 0; i < oidParts.length; i++) {
            if (i > 0) pathBuilder.append(".");
            pathBuilder.append(oidParts[i]);
            String path = pathBuilder.toString();

            DefaultMutableTreeNode node = null;
            // Find if node already exists (shouldn't, but for safety)
            for (int j = 0; j < parent.getChildCount(); j++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(j);
                if (child.getUserObject().equals(path)) {
                    node = child;
                    break;
                }
            }
            if (node == null) {
                node = new DefaultMutableTreeNode(path);
                parent.add(node);
            }
            parent = node;
        }
        // Add the value as a leaf node
        DefaultMutableTreeNode valueNode = new DefaultMutableTreeNode(oid + " = " + value);
        parent.add(valueNode);

        walkTreeModel.reload();
        walkTree.expandRow(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SNMPBrowserUI().setVisible(true);
        });
    }
}
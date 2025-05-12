package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;


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

    // Status components
    private JLabel statusLabel;
    private JProgressBar operationProgress;

    // Core functionality provider
    private SNMPManager snmpManager;

    // Track the current OID path for navigation
    private String currentOidPath = "";
    private JButton backButton;

    public SNMPBrowserUI() {
        super("SNMP Browser");

        // Initialize the manager
        this.snmpManager = new SNMPManager();

        initializeUI();
        loadInitialData();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Create main tabbed pane
        tabbedPane = new JTabbedPane();

        // Create the different tabs
        tabbedPane.addTab("Quick Query", createQueryPanel());
        tabbedPane.addTab("MIB Browser", createMIBBrowserPanel());
        tabbedPane.addTab("Results", createResultsPanel());

        // Add status bar
        JPanel statusPanel = createStatusPanel();

        // Add to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createToolBar(), BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(statusPanel, BorderLayout.SOUTH);
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

        // Results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("Results"));

        // Add action listeners
        getButton.addActionListener(e -> performGet());
        getNextButton.addActionListener(e -> performGetNext());
        walkButton.addActionListener(e -> performWalk());
        smartQueryButton.addActionListener(e -> performSmartQuery());
        recentTargetsCombo.addActionListener(e -> handleRecentTargetSelection());

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(resultScroll, BorderLayout.CENTER);

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

        // Create detail panel
        JPanel detailPanel = new JPanel(new BorderLayout());
        JTextArea nodeDetailArea = new JTextArea();
        nodeDetailArea.setEditable(false);
        JScrollPane detailScroll = new JScrollPane(nodeDetailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Node Details"));

        // Action buttons
        JPanel mibActionsPanel = new JPanel();
        JButton importMibButton = new JButton("Import MIB");
        JButton setMibDirButton = new JButton("Set MIB Directory");
        JButton querySelectedButton = new JButton("Query Selected");

        mibActionsPanel.add(importMibButton);
        mibActionsPanel.add(setMibDirButton);
        mibActionsPanel.add(querySelectedButton);

        detailPanel.add(detailScroll, BorderLayout.CENTER);
        detailPanel.add(mibActionsPanel, BorderLayout.SOUTH);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, mibsScroll, detailPanel);
        splitPane.setDividerLocation(300);

        // Add listeners
        searchButton.addActionListener(e -> searchMibs(searchField.getText()));
        importMibButton.addActionListener(e -> importMib());
        setMibDirButton.addActionListener(e -> setMibDirectory());
        querySelectedButton.addActionListener(e -> querySelectedNode());
        
        // Double-click to navigate/select
        mibsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = mibsList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        String selectedNode = mibsListModel.getElementAt(index);
                        if (selectedNode != null) {
                            // Extract OID from selection (assuming format "OID (MIB_NAME)" or just "OID")
                            String oid = selectedNode;
                            if (selectedNode.contains(" (")) {
                                oid = selectedNode.substring(0, selectedNode.indexOf(" ("));
                            }
                            java.util.List<org.example.Node> children = snmpManager.getChildrenOfOid(oid);
                            if (children != null && !children.isEmpty()) {
                                // Navigate into this node
                                currentOidPath = oid;
                                updateMibsListWithChildren(children);
                                backButton.setEnabled(true);
                            } else {
                                // No children, show node details as before
                                displayNodeDetails(selectedNode, nodeDetailArea);
                            }
                        }
                    }
                }
            }
        });
        
        // Back button logic
        backButton.addActionListener(e -> {
            if (currentOidPath == null || currentOidPath.isEmpty()) return;
            // Remove last part of OID path
            int lastDot = currentOidPath.lastIndexOf('.');
            if (lastDot > 0) {
                currentOidPath = currentOidPath.substring(0, lastDot);
            } else {
                currentOidPath = "";
            }
            java.util.List<org.example.Node> children = snmpManager.getChildrenOfOid(currentOidPath);
            updateMibsListWithChildren(children);
            backButton.setEnabled(!currentOidPath.isEmpty());
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model
        String[] columns = {"OID", "Value", "Type", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        queryTable = new JTable(tableModel);
        queryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(queryTable);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton exportButton = new JButton("Export to CSV");
        JButton clearButton = new JButton("Clear Results");

        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);

        // Add listeners
        exportButton.addActionListener(e -> exportToCSV());
        clearButton.addActionListener(e -> clearResults());

        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        queryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = queryTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        showRowDetailsDialog(row);
                    }
                }
            }
        });

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
                mibsListModel.addElement(child.oid + (child.name != null ? " (" + child.name + ")" : ""));
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
            resultArea.append(result + "\n");

            // Add to results table
            addResultToTable(oid, result);

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
                resultArea.append(result.get(0) + "\n");

                // Update OID field with next OID
                if (result.size() > 1) {
                    oidField.setText(result.get(1));
                }

                // Add to results table
                addResultToTable(oid, result.get(0));
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

    private void performWalk() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            setInProgress("Performing Walk operation...");

            List<String> results = snmpManager.walk(ipAddress, port, community, oid);

            for (String result : results) {
                resultArea.append(result + "\n");

                // Add to results table
                addResultToTable(oid, result);
            }

            setCompleted("Walk operation completed - " + results.size() + " results");
        } catch (NumberFormatException e) {
            UIHelper.showError("Error", "Invalid port number");
            setCompleted("Error: Invalid port number");
        } catch (Exception e) {
            UIHelper.showError("Error", e.getMessage());
            setCompleted("Error: " + e.getMessage());
        }
    }

    private void addResultToTable(String oid, String result) {
        // Parse the result string to extract information
        String[] parts = result.split(" = ");
        if (parts.length < 2) return;

        String resultOid = parts[0];
        String value = parts[1].trim();

        // Filter out SNMP errors
        if (value.equalsIgnoreCase("noSuchInstance") || value.equalsIgnoreCase("noSuchObject")) {
            // Optionally, show a message or skip adding to the table
            // JOptionPane.showMessageDialog(this, "OID " + resultOid + " is not a valid instance or object.", "SNMP Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get node information if available
        Node node = snmpManager.lookupNode(resultOid);
        if (node == null) {
            // Try to get the base OID (for tabular data)
            int lastDot = resultOid.lastIndexOf('.');
            if (lastDot > 0) {
                String baseOid = resultOid.substring(0, lastDot);
                node = snmpManager.lookupNode(baseOid);
            }
        }
        String type = (node != null) ? node.type : "";
        String description = (node != null) ? node.description : "";

        // Add to table
        tableModel.addRow(new Object[]{resultOid, value, type, description});

        // Switch to Results tab
        tabbedPane.setSelectedIndex(2);
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

    private void setMibDirectory() {
        snmpManager.setMibDirectory();
        // Refresh the list for the current node
        List<org.example.Node> children = snmpManager.getChildrenOfOid(currentOidPath);
        updateMibsListWithChildren(children);
    }

    private void querySelectedNode() {
        String selectedNode = mibsList.getSelectedValue();
        if (selectedNode != null) {
            // Extract OID from selection
            String oid = selectedNode;
            if (selectedNode.contains(" (")) {
                oid = selectedNode.substring(0, selectedNode.indexOf(" ("));
            }

            // Set the OID in the query tab
            oidField.setText(oid);

            // Switch to query tab
            tabbedPane.setSelectedIndex(0);
        }
    }

    private void exportToCSV() {
        snmpManager.saveQueryResultsToCSV();
    }

    private void clearResults() {
        int rowCount = tableModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            tableModel.removeRow(i);
        }
        statusLabel.setText("Results cleared");
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
                resultArea.append(result + "\n");
                addResultToTable(oid, result);
            } else {
                List<String> results = snmpManager.walk(ipAddress, port, community, oid);
                for (String result : results) {
                    resultArea.append(result + "\n");
                    addResultToTable(oid, result);
                }
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

    private void showRowDetailsDialog(int row) {
        String oid = tableModel.getValueAt(row, 0) != null ? tableModel.getValueAt(row, 0).toString() : "";
        String value = tableModel.getValueAt(row, 1) != null ? tableModel.getValueAt(row, 1).toString() : "";
        String type = tableModel.getValueAt(row, 2) != null ? tableModel.getValueAt(row, 2).toString() : "";
        String description = tableModel.getValueAt(row, 3) != null ? tableModel.getValueAt(row, 3).toString() : "";

        JTextArea textArea = new JTextArea(
            "OID: " + oid + "\n\n" +
            "Value: " + value + "\n\n" +
            "Type: " + type + "\n\n" +
            "Description: " + description
        );
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 200)); // 2 width * 4 height (approx)

        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Row Details",
            JOptionPane.INFORMATION_MESSAGE
        );
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
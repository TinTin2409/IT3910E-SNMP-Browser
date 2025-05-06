package org.example;

import javax.swing.*;
import java.awt.*;

public class SNMPBrowserUI {
    private final JTextField ipAddressField;
    private final JTextField portField;
    private final JTextField communityField;
    private final JTextField oidField;
    private final JTextArea resultArea;
    private final SNMPManager snmpManager;
    private final MibLoader mibLoader;

    public SNMPBrowserUI() {
        // Create the main frame
        JFrame frame = new JFrame("SNMP Browser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize components
        ipAddressField = new JTextField("127.0.0.1", 20);
        portField = new JTextField("161", 20);
        communityField = new JTextField("public", 20);
        oidField = new JTextField("1.3.6.1.2.1.1.1.0", 20);
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);

        // Initialize MibLoader
        this.mibLoader = new MibLoader();
        try {
            this.mibLoader.loadMibsFromFolder("mibs");
        } catch (Exception e) {
            System.err.println("Warning: Could not load MIBs: " + e.getMessage());
        }

        // Initialize SNMPManager
        this.snmpManager = new SNMPManager(this.mibLoader);

        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Add components to form
        addFormRow(formPanel, "IP Address:", ipAddressField, gbc, 0);
        addFormRow(formPanel, "Port:", portField, gbc, 1);
        addFormRow(formPanel, "Community:", communityField, gbc, 2);
        addFormRow(formPanel, "OID:", oidField, gbc, 3);

        // Add Get button
        JButton getButton = new JButton("Get");
        gbc.gridx = 1;
        gbc.gridy = 4;
        formPanel.add(getButton, gbc);

        // Add action listener to the button
        getButton.addActionListener(e -> performGet());

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Add main panel to frame
        frame.add(mainPanel);

        // Set frame properties
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addFormRow(JPanel panel, String label, JComponent field, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(field, gbc);
    }

    private void performGet() {
        try {
            String ipAddress = ipAddressField.getText();
            int port = Integer.parseInt(portField.getText());
            String community = communityField.getText();
            String oid = oidField.getText();

            String result = snmpManager.get(ipAddress, port, community, oid);
            resultArea.append(result + "\n");
        } catch (NumberFormatException e) {
            resultArea.append("Error: Invalid port number\n");
        } catch (Exception e) {
            resultArea.append("Error: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SNMPBrowserUI();
        });
    }
}
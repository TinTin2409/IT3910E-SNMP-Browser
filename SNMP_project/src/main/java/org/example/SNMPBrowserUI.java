package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SNMPBrowserUI extends JFrame {
    private final SNMPManager snmpManager;
    private final JTextField ipField;
    private final JTextField portField;
    private final JTextField communityField;
    private final JTextField oidField;
    private final JTextArea resultArea;

    public SNMPBrowserUI() {
        this.snmpManager = new SNMPManager();
        setTitle("SNMP Browser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Create input panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // IP Address
        inputPanel.add(new JLabel("IP Address:"));
        ipField = new JTextField("localhost");
        inputPanel.add(ipField);

        // Port
        inputPanel.add(new JLabel("Port:"));
        portField = new JTextField("161");
        inputPanel.add(portField);

        // Community
        inputPanel.add(new JLabel("Community:"));
        communityField = new JTextField("public");
        inputPanel.add(communityField);

        // OID
        inputPanel.add(new JLabel("OID:"));
        oidField = new JTextField("1.3.6.1.2.1.1.1.0");
        inputPanel.add(oidField);

        // Result area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Get button
        JButton getButton = new JButton("Get");
        getButton.addActionListener(e -> performGet());

        // Main layout
        setLayout(new BorderLayout(5, 5));
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(getButton, BorderLayout.SOUTH);
    }

    private void performGet() {
        try {
            String result = snmpManager.get(
                ipField.getText(),
                Integer.parseInt(portField.getText()),
                communityField.getText(),
                oidField.getText()
            );
            resultArea.append(result + "\n");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error: " + ex.getMessage(),
                "SNMP Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Invalid port number",
                "Input Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SNMPBrowserUI browser = new SNMPBrowserUI();
            browser.setVisible(true);
        });
    }
}
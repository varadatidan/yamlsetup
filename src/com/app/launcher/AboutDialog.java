package com.app.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

public class AboutDialog extends JDialog {

    public AboutDialog(JFrame parent) {
        super(parent, "About Automation Orchestrator", true);
        setSize(450, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. App Icon / Logo Placeholder
        JLabel logo = new JLabel("🤖", JLabel.CENTER);
        logo.setFont(new Font("Segoe UI", Font.PLAIN, 60));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. App Name & Version
        JLabel title = new JLabel("YamlAutoTool v2.0");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(82, 177, 245));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Developer Info
        JLabel devInfo = new JLabel("Lead Developer: Varad");
        devInfo.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        devInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 4. Description Area
        JTextPane description = new JTextPane();
        description.setText("\nA GenAI-powered automation framework designed for rapid Azure MuiDataGrid validation. \n\n" +
                           "Features:\n" +
                           "• Keyword-Driven YAML Execution\n" +
                           "• Gemini 2.5 Flash AI Script Architect\n" +
                           "• Extent Report Integration\n" +
                           "• Headless Execution Support");
        description.setEditable(false);
        description.setBackground(new Color(0,0,0,0)); // Transparent
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(logo);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(title);
        contentPanel.add(devInfo);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(description);

        // 5. Close Button
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeBtn);

        add(contentPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
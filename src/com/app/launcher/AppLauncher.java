package com.app.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.yamlautotool.runner.*;
import reports.reports.Project_Reports;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppLauncher {
    private static DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private static List<String> absolutePaths = new ArrayList<>();

    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch (Exception e) {}

        JFrame frame = new JFrame("Automation Control Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 650);
        
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel header = new JLabel("Automation Orchestrator", JLabel.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 30));
        header.setForeground(new Color(82, 177, 245));
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setPreferredSize(new Dimension(380, 0));
        JList<String> fileList = new JList<>(fileListModel);
        fileList.setBackground(new Color(45, 45, 45));
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Execution Queue"));

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton addBtn = new JButton("➕ Add");
        JButton clearBtn = new JButton("🗑️ Clear");
        JButton reportsBtn = new JButton("📂 Reports");
        reportsBtn.setForeground(new Color(82, 177, 245));
        btnPanel.add(addBtn); btnPanel.add(clearBtn); btnPanel.add(reportsBtn);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        JPanel configPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        String[] modes = { "Standard Runner (Creation)", "Grid Runner (Validation)" };
        JComboBox<String> modeDropdown = new JComboBox<>(modes);
        JCheckBox headlessBox = new JCheckBox("Run in Headless Mode");
        configPanel.add(new JLabel("Select Runner Engine Mode:"));
        configPanel.add(modeDropdown); configPanel.add(headlessBox);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(new Color(137, 209, 133));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Live Execution Logs"));

        JButton runBtn = new JButton("🚀 RUN BATCH");
        runBtn.setPreferredSize(new Dimension(0, 65));
        runBtn.setBackground(new Color(27, 114, 168));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        runBtn.setEnabled(false);

        rightPanel.add(configPanel, BorderLayout.NORTH);
        rightPanel.add(logScroll, BorderLayout.CENTER);
        rightPanel.add(runBtn, BorderLayout.SOUTH);

        // Actions
        addBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    if (!absolutePaths.contains(f.getAbsolutePath())) {
                        fileListModel.addElement(f.getName());
                        absolutePaths.add(f.getAbsolutePath());
                    }
                }
                runBtn.setEnabled(!absolutePaths.isEmpty());
            }
        });

        clearBtn.addActionListener(e -> {
            fileListModel.clear(); absolutePaths.clear(); runBtn.setEnabled(false); logArea.setText("");
        });

        reportsBtn.addActionListener(e -> {
            try { Desktop.getDesktop().open(new File(System.getProperty("user.dir") + "/Reports")); } catch (Exception ex) {}
        });

        runBtn.addActionListener(e -> {
            runBtn.setEnabled(false); addBtn.setEnabled(false); clearBtn.setEnabled(false);
            String mode = (String) modeDropdown.getSelectedItem();
            boolean headless = headlessBox.isSelected();

            new Thread(() -> {
                Project_Reports runner = null;
                try {
                    runner = mode.contains("Grid") ? new GridTestRunner() : new TestRunner();
                    
                    // --- THE LOGGER LINK ---
                    runner.uiLogger = (msg) -> SwingUtilities.invokeLater(() -> {
                        logArea.append(msg);
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });

                    runner.log("▶️ Initializing " + mode + "...");
                    runner.isHeadless = headless;
                    runner.setupSuite();
                    runner.setupBrowser();

                    for (String path : absolutePaths) {
                        runner.log("⏳ Processing: " + new File(path).getName());
                        runner.executeYaml(path);
                    }

                    runner.extent.flush();
                    if (runner.driver != null) runner.driver.quit();
                    runner.log("✅ Batch Finished!");
                    Desktop.getDesktop().open(new File(runner.reportDir));
                } catch (Exception ex) {
                    if (runner != null) runner.log("❌ ERROR: " + ex.getMessage());
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        runBtn.setEnabled(true); addBtn.setEnabled(true); clearBtn.setEnabled(true);
                    });
                }
            }).start();
        });

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        frame.add(mainPanel); frame.setLocationRelativeTo(null); frame.setVisible(true);
    }
}
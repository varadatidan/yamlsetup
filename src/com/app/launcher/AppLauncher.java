package com.app.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.yamlautotool.runner.TestRunner;
import com.yamlautotool.runner.GridTestRunner;
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
        // Apply Modern Dark Theme
        try {
            FlatDarkLaf.setup(); 
        } catch (Exception e) {
            System.err.println("FlatLaf failed to initialize.");
        }

        JFrame frame = new JFrame("ResIQ Automation Control Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // --- HEADER ---
        JLabel header = new JLabel("ResIQ Automation Engine", JLabel.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 28));
        header.setForeground(new Color(82, 177, 245));
        mainPanel.add(header, BorderLayout.NORTH);

        // --- LEFT PANEL: Queue Management ---
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setPreferredSize(new Dimension(380, 0));
        
        JList<String> fileList = new JList<>(fileListModel);
        fileList.setBackground(new Color(40, 40, 40));
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Execution Queue"));

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton addBtn = new JButton("➕ Add YAMLs");
        JButton clearBtn = new JButton("🗑️ Clear All");
        btnPanel.add(addBtn);
        btnPanel.add(clearBtn);

        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        // --- RIGHT PANEL: Configuration & Logs ---
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel configPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        String[] modes = { "Standard Runner (Creation)", "Grid Runner (Validation)" };
        JComboBox<String> modeDropdown = new JComboBox<>(modes);
        configPanel.add(new JLabel("Runner Engine Mode:"));
        configPanel.add(modeDropdown);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 25, 25));
        logArea.setForeground(new Color(137, 209, 133));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Live Execution Logs"));

        JButton runBtn = new JButton("🚀 RUN BATCH");
        runBtn.setPreferredSize(new Dimension(0, 60));
        runBtn.setBackground(new Color(27, 114, 168));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        runBtn.setEnabled(false);

        rightPanel.add(configPanel, BorderLayout.NORTH);
        rightPanel.add(logScroll, BorderLayout.CENTER);
        rightPanel.add(runBtn, BorderLayout.SOUTH);

        // --- LOGIC ---

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
            fileListModel.clear();
            absolutePaths.clear();
            runBtn.setEnabled(false);
            logArea.setText("");
        });

        runBtn.addActionListener(e -> {
            runBtn.setEnabled(false);
            addBtn.setEnabled(false);
            clearBtn.setEnabled(false);
            String mode = (String) modeDropdown.getSelectedItem();
            
            new Thread(() -> {
                try {
                    logArea.append("▶️ Initializing Batch Execution...\n");

                    if (mode.contains("Grid")) {
                        GridTestRunner runner = new GridTestRunner();
                        
                        // INITIALIZE ONCE (The fix for missing reports)
                        logArea.append("⚙️ Setup: Report & Browser Session...\n");
                        runner.setupSuite();
                        runner.setupBrowser();

                        for (String path : absolutePaths) {
                            logArea.append("⏳ Testing: " + new File(path).getName() + "\n");
                            runner.executeYaml(path);
                        }

                        // FLUSH & CLOSE (Ensure reports are saved)
                        runner.extent.flush();
                        runner.driver.quit();
                        logArea.append("✅ Batch Finished! Opening Folder...\n");
                        Desktop.getDesktop().open(new File(GridTestRunner.reportDir));

                    } else {
                        TestRunner runner = new TestRunner();
                        runner.setupSuite();
                        runner.setupBrowser();

                        for (String path : absolutePaths) {
                            logArea.append("⏳ Testing: " + new File(path).getName() + "\n");
                            runner.executeYaml(path);
                        }

                        runner.extent.flush();
                        runner.driver.quit();
                        logArea.append("✅ Batch Finished! Opening Folder...\n");
                        Desktop.getDesktop().open(new File(TestRunner.reportDir));
                    }

                    JOptionPane.showMessageDialog(frame, "Batch execution completed successfully!");
                } catch (Exception ex) {
                    logArea.append("❌ CRITICAL ERROR: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                } finally {
                    runBtn.setEnabled(true);
                    addBtn.setEnabled(true);
                    clearBtn.setEnabled(true);
                }
            }).start();
        });

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
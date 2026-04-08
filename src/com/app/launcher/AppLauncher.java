package com.app.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.yamlautotool.runner.*;
import com.yamlautotool.utils.AIClient;
import reports.reports.Project_Reports;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppLauncher {
    private static DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private static List<String> absolutePaths = new ArrayList<>();
    private static JTextField urlInput; // Target URL for AI

    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch (Exception e) {}

        JFrame frame = new JFrame("Automation Control Center - GenAI Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 800);
        
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0,0,0,0));
        JLabel header = new JLabel("Automation Orchestrator", JLabel.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 30));
        header.setForeground(new Color(82, 177, 245));
        mainPanel.add(header, BorderLayout.NORTH);
        JButton infoBtn = new JButton("ℹ️ About");
        infoBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoBtn.addActionListener(e -> new AboutDialog(frame).setVisible(true));
        headerPanel.add(header, BorderLayout.WEST);
        // --- LEFT PANEL (Queue + AI) ---
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setPreferredSize(new Dimension(400, 0));
        headerPanel.add(infoBtn, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JList<String> fileList = new JList<>(fileListModel);
        fileList.setBackground(new Color(45, 45, 45));
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Execution Queue"));

        // --- AI GENERATION PANEL (URL Field Integrated Here) ---
        JPanel aiPanel = new JPanel();
        aiPanel.setLayout(new BoxLayout(aiPanel, BoxLayout.Y_AXIS));
        aiPanel.setBorder(BorderFactory.createTitledBorder("GenAI Script Architect"));
        
        // 1. URL Input Section
        JPanel urlRow = new JPanel(new BorderLayout(5, 5));
        urlRow.add(new JLabel("Target URL:"), BorderLayout.NORTH);
        urlInput = new JTextField("Add Url For Ai Genration");
        urlRow.add(urlInput, BorderLayout.CENTER);
        
        // 2. Intent Input Section
        JPanel intentRow = new JPanel(new BorderLayout(5, 5));
        intentRow.add(new JLabel("Describe your test case:"), BorderLayout.NORTH);
        JTextField aiInputField = new JTextField();
        aiInputField.setToolTipText("e.g., 'Search for Varad and Export'");
        intentRow.add(aiInputField, BorderLayout.CENTER);
        
        // 3. Generate Button
        JButton aiGenBtn = new JButton("✨ Generate & Add");
        aiGenBtn.setBackground(new Color(106, 58, 152)); 
        aiGenBtn.setForeground(Color.WHITE);
        aiGenBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Add components to the AI Panel with spacing
        aiPanel.add(urlRow);
        aiPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        aiPanel.add(intentRow);
        aiPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        aiPanel.add(aiGenBtn);

        // BUTTON PANEL (Add/Clear/Reports)
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton addBtn = new JButton("➕ Add");
        JButton clearBtn = new JButton("🗑️ Clear");
        JButton reportsBtn = new JButton("📂 Reports");
        reportsBtn.setForeground(new Color(82, 177, 245));
        btnPanel.add(addBtn); btnPanel.add(clearBtn); btnPanel.add(reportsBtn);

        JPanel leftContainer = new JPanel(new BorderLayout(10, 10));
        leftContainer.add(listScroll, BorderLayout.CENTER);
        
        JPanel leftBottomGroup = new JPanel(new BorderLayout(10, 10));
        leftBottomGroup.add(aiPanel, BorderLayout.NORTH);
        leftBottomGroup.add(btnPanel, BorderLayout.SOUTH);
        
        leftPanel.add(leftContainer, BorderLayout.CENTER);
        leftPanel.add(leftBottomGroup, BorderLayout.SOUTH);

        // --- RIGHT PANEL (Logs) ---
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

        // --- LOGIC: AI GENERATOR ---
        aiGenBtn.addActionListener(e -> {
            String intent = aiInputField.getText().trim();
            String currentBaseUrl = urlInput.getText().trim();
            
            if (intent.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please describe the test intent!");
                return;
            }

            aiGenBtn.setEnabled(false);
            aiGenBtn.setText("🧠 Thinking...");

            new Thread(() -> {
                try {
                    String prompt = "You are a QA Architect. Generate a YAML test script.\n" +
                            "CRITICAL STRUCTURE REQUIRED:\n" +
                            "name: \"[Test Name]\"\n" +
                            "steps:\n" +
                            "  - name: \"[Step Description]\"\n" +
                            "    action: \"[click|input|navigate|wait|screenshot]\"\n" +
                            "    locator: \"xpath=[Value]\"\n" +
                            "    value: \"[Value]\"\n\n" +
                            "USER INTENT: " + intent + "\n" +
                            "BASE URL: " + currentBaseUrl + "\n" +
                            "RULES:\n" +
                            "1. Start with a 'navigate' step using the Base URL.\n" +
                            "2. Output ONLY raw YAML.";

                    String generatedYaml = AIClient.callGemini(prompt);
                    String fileName = "ai_test_" + System.currentTimeMillis() + ".yaml";
                    String fullPath = Paths.get(System.getProperty("user.dir"), "resources", fileName).toString();
                    
                    try (FileWriter writer = new FileWriter(fullPath)) {
                        writer.write(generatedYaml);
                    }

                    SwingUtilities.invokeLater(() -> {
                        fileListModel.addElement("✨ " + fileName);
                        absolutePaths.add(fullPath);
                        runBtn.setEnabled(true);
                        aiInputField.setText("");
                        aiGenBtn.setText("✨ Generate & Add");
                        aiGenBtn.setEnabled(true);
                        logArea.append("🤖 AI Architect: Test generated for -> " + currentBaseUrl + "\n");
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        aiGenBtn.setEnabled(true);
                        aiGenBtn.setText("✨ Generate & Add");
                    });
                }
            }).start();
        });

        // --- OTHER ACTIONS ---
        addBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    fileListModel.addElement(f.getName());
                    absolutePaths.add(f.getAbsolutePath());
                }
                runBtn.setEnabled(true);
            }
        });

        clearBtn.addActionListener(e -> {
            fileListModel.clear(); absolutePaths.clear(); runBtn.setEnabled(false); logArea.setText("");
        });

        reportsBtn.addActionListener(e -> {
            try { Desktop.getDesktop().open(new File(System.getProperty("user.dir") + "/Reports")); } catch (Exception ex) {}
        });

        runBtn.addActionListener(e -> {
            runBtn.setEnabled(false);
            new Thread(() -> {
                Project_Reports runner = null;
                try {
                    String mode = (String) modeDropdown.getSelectedItem();
                    runner = mode.contains("Grid") ? new GridTestRunner() : new TestRunner();
                    runner.uiLogger = (msg) -> SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
                    runner.isHeadless = headlessBox.isSelected();
                    runner.setupSuite(); runner.setupBrowser();

                    for (String path : absolutePaths) {
                        runner.executeYaml(path);
                    }
                    runner.extent.flush();
                    if (runner.driver != null) runner.driver.quit();
                    Desktop.getDesktop().open(new File(runner.reportDir));
                } catch (Exception ex) {
                    logArea.append("❌ ERROR: " + ex.getMessage() + "\n");
                } finally {
                    SwingUtilities.invokeLater(() -> runBtn.setEnabled(true));
                }
            }).start();
        });

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        frame.add(mainPanel); frame.setLocationRelativeTo(null); frame.setVisible(true);
    }
}
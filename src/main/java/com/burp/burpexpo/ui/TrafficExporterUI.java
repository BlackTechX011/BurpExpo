/*
 * ==============================================================================
 * Project:     BurpExpo
 * Author:      @BlackTechX011
 * Repository:  https://github.com/BlackTechX011/BurpExpo
 * 
 * Description: Advanced Burp Suite traffic exporter with real-time logging, 
 *              deduplication, AI map generation, and multi-language 
 *              code snippet generation.
 * ==============================================================================
 */
 
package com.burp.burpexpo.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import com.burp.burpexpo.filter.DeduplicationFilter;
import com.burp.burpexpo.io.MapGenerator;
import com.burp.burpexpo.utils.SnippetGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrafficExporterUI {
    private final MontoyaApi api;
    private JPanel mainPanel;
    
    // UI Components - Config
    private JTextField pathField;
    private JCheckBox enableLoggingCheck;
    private JCheckBox mapCheck;
    
    // UI Components - Filters
    private JCheckBox dedupeCheck;
    private JCheckBox scopeCheck;
    private JTextField excludeExtField;
    private JTextField includeHostsField;
    private JTextField excludeHostsField;
    private JTextField methodsField;
    private JTextField statusCodesField;
    
    // UI Components - Controls & Status
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel statsLabel;

    // AI & Filtering
    private final MapGenerator mapGenerator;
    private final DeduplicationFilter dedupeFilter;

    // Table
    private JTable logTable;
    private DefaultTableModel tableModel;
    
    // Store original requests for snippet generation in UI
    private final List<HttpRequest> loggedRequests = new ArrayList<>();

    // State
    private int fileCounter = 0;
    private int sessionExportCount = 0;
    private String currentSessionDir = "";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat sessionFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // Colors
    private final Color PRIMARY_COLOR = new Color(52, 152, 219);
    private final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private final Color ACCENT_COLOR = new Color(52, 73, 94);
    private final Color BG_COLOR = new Color(245, 247, 250);

    public TrafficExporterUI(MontoyaApi api) {
        this.api = api;
        this.mapGenerator = new MapGenerator(api.logging());
        this.dedupeFilter = new DeduplicationFilter();
        initComponents();
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- TOP PANEL: Config & Filters ---
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Output Path
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(createStyledLabel("Base Output Directory:"), gbc);
        pathField = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        topPanel.add(pathField, gbc);
        JButton browseBtn = createStyledButton("Browse", null);
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx = 2; gbc.weightx = 0;
        topPanel.add(browseBtn, gbc);

        // Advanced Filters Panel
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        TitledBorder filterBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Advanced Filters");
        filterBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        filterBorder.setTitleColor(ACCENT_COLOR);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(filterBorder, new EmptyBorder(5, 5, 5, 5)));

        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.insets = new Insets(2, 5, 2, 5);
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        fGbc.anchor = GridBagConstraints.WEST;

        // Row 1: Checkboxes
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        checkPanel.setOpaque(false);
        dedupeCheck = new JCheckBox("Deduplicate Requests", true);
        scopeCheck = new JCheckBox("In-Scope Only", false);
        mapCheck = new JCheckBox("Generate AI Maps (JSON/TXT)", true);
        dedupeCheck.setOpaque(false); scopeCheck.setOpaque(false); mapCheck.setOpaque(false);
        checkPanel.add(dedupeCheck); checkPanel.add(scopeCheck); checkPanel.add(mapCheck);
        fGbc.gridx = 0; fGbc.gridy = 0; fGbc.gridwidth = 4;
        filterPanel.add(checkPanel, fGbc);

        // Row 2: Text Filters (Left column)
        fGbc.gridwidth = 1; fGbc.gridy = 1; fGbc.gridx = 0;
        filterPanel.add(createStyledLabel("Include Hosts:"), fGbc);
        includeHostsField = new JTextField();
        includeHostsField.setToolTipText("e.g. api.example.com, mytarget (Empty = All)");
        fGbc.gridx = 1; fGbc.weightx = 0.5;
        filterPanel.add(includeHostsField, fGbc);

        // Row 2: Text Filters (Right column)
        fGbc.gridx = 2; fGbc.weightx = 0;
        filterPanel.add(createStyledLabel("Exclude Hosts:"), fGbc);
        excludeHostsField = new JTextField("analytics, tracking");
        excludeHostsField.setToolTipText("e.g. analytics.com, google (Empty = None)");
        fGbc.gridx = 3; fGbc.weightx = 0.5;
        filterPanel.add(excludeHostsField, fGbc);

        // Row 3: Text Filters (Left column)
        fGbc.gridx = 0; fGbc.gridy = 2; fGbc.weightx = 0;
        filterPanel.add(createStyledLabel("HTTP Methods:"), fGbc);
        methodsField = new JTextField();
        methodsField.setToolTipText("e.g. GET, POST, PUT (Empty = All)");
        fGbc.gridx = 1; fGbc.weightx = 0.5;
        filterPanel.add(methodsField, fGbc);

        // Row 3: Text Filters (Right column)
        fGbc.gridx = 2; fGbc.weightx = 0;
        filterPanel.add(createStyledLabel("Status Codes:"), fGbc);
        statusCodesField = new JTextField();
        statusCodesField.setToolTipText("e.g. 200, 301, 4xx, 5xx (Empty = All)");
        fGbc.gridx = 3; fGbc.weightx = 0.5;
        filterPanel.add(statusCodesField, fGbc);

        // Row 4: Extensions (Span full)
        fGbc.gridx = 0; fGbc.gridy = 3; fGbc.weightx = 0;
        filterPanel.add(createStyledLabel("Exclude Exts:"), fGbc);
        excludeExtField = new JTextField(".css, .js, .png, .jpg, .jpeg, .gif, .woff, .woff2, .ico, .svg, .map");
        fGbc.gridx = 1; fGbc.gridwidth = 3; fGbc.weightx = 1.0;
        filterPanel.add(excludeExtField, fGbc);

        // Add filter panel to top panel
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        topPanel.add(filterPanel, gbc);

        // Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setOpaque(false);
        JButton newSessionBtn = createStyledButton("Start New Session", SUCCESS_COLOR);
        newSessionBtn.addActionListener(e -> startNewSession());
        
        enableLoggingCheck = new JCheckBox("Enable Real-Time Export", false);
        enableLoggingCheck.setOpaque(false);
        enableLoggingCheck.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JButton exportHistoryBtn = createStyledButton("Export Proxy History", PRIMARY_COLOR);
        exportHistoryBtn.addActionListener(e -> startHistoryExport());
        
        JButton openFolderBtn = createStyledButton("Open Session Folder", ACCENT_COLOR);
        openFolderBtn.addActionListener(e -> openOutputFolder());
        
        controlPanel.add(newSessionBtn);
        controlPanel.add(enableLoggingCheck);
        controlPanel.add(exportHistoryBtn);
        controlPanel.add(openFolderBtn);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        topPanel.add(controlPanel, gbc);

        // Status
        statusLabel = new JLabel("Status: Idle | No active session");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setForeground(SUCCESS_COLOR);
        
        gbc.gridy = 3; topPanel.add(statusLabel, gbc);
        gbc.gridy = 4; topPanel.add(progressBar, gbc);

        // --- BOTTOM PANEL: Table ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        String[] columns = {"ID", "Time", "Method", "Host", "Path", "Status", "Saved In"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        logTable = new JTable(tableModel);
        logTable.setRowHeight(30);
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        logTable.getTableHeader().setBackground(ACCENT_COLOR);
        logTable.getTableHeader().setForeground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(logTable);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel tableStatsPanel = new JPanel(new BorderLayout());
        tableStatsPanel.setOpaque(false);
        statsLabel = new JLabel("Total Items Exported: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableStatsPanel.add(statsLabel, BorderLayout.WEST);
        
        JButton clearTableBtn = new JButton("Clear Table View");
        clearTableBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            loggedRequests.clear();
        });
        tableStatsPanel.add(clearTableBtn, BorderLayout.EAST);
        
        bottomPanel.add(tableStatsPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        splitPane.setDividerLocation(380);
        splitPane.setBorder(null);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        setupPopupMenu();
    }

    private JLabel createStyledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(ACCENT_COLOR);
        return l;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); }
        b.setFocusPainted(false);
        return b;
    }

    private void setupPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyHost = new JMenuItem("Copy Host");
        copyHost.addActionListener(e -> copySelected(3));
        JMenuItem copyPath = new JMenuItem("Copy Path");
        copyPath.addActionListener(e -> copySelected(4));
        
        JMenu snippetMenu = new JMenu("Export Snippet (Clipboard)");
        String[] languages = {"cURL", "Python Request", "Wget", "Go Native", "NodeJS Request", "PowerShell", "PHP HTTP_Request2", "Javascript XHR"};
        for (String lang : languages) {
            JMenuItem langItem = new JMenuItem(lang);
            langItem.addActionListener(e -> copySnippet(lang));
            snippetMenu.add(langItem);
        }

        popup.add(copyHost);
        popup.add(copyPath);
        popup.addSeparator();
        popup.add(snippetMenu);
        
        logTable.setComponentPopupMenu(popup);
    }

    private void copySelected(int col) {
        int row = logTable.getSelectedRow();
        if (row != -1) {
            String val = logTable.getValueAt(row, col).toString();
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(val), null);
        }
    }
    
    private void copySnippet(String language) {
        int row = logTable.getSelectedRow();
        if (row != -1 && row < loggedRequests.size()) {
            HttpRequest req = loggedRequests.get(row);
            String code = SnippetGenerator.generateCode(req, language);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        }
    }

    private void startNewSession() {
        String basePath = pathField.getText().trim();
        if (basePath.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "Please select an Output Directory!");
            return;
        }
        String sessionName = "Session_" + sessionFormat.format(new Date());
        File sessionDir = new File(basePath, sessionName);
        if (sessionDir.mkdirs()) {
            currentSessionDir = sessionDir.getAbsolutePath();
            fileCounter = 0; sessionExportCount = 0;
            statusLabel.setText("Status: Session active | " + sessionName);
            updateStats();
        }
    }

    private void updateStats() {
        SwingUtilities.invokeLater(() -> statsLabel.setText("Total Session Items: " + sessionExportCount));
    }

    private void openOutputFolder() {
        if (currentSessionDir.isEmpty()) return;
        try { Desktop.getDesktop().open(new File(currentSessionDir)); } catch (Exception ignored) {}
    }

    private void startHistoryExport() {
        if (currentSessionDir.isEmpty()) startNewSession();
        if (currentSessionDir.isEmpty()) return;

        progressBar.setVisible(true);
        progressBar.setValue(0);
        new Thread(() -> {
            try {
                List<ProxyHttpRequestResponse> history = api.proxy().history();
                int total = history.size();
                SwingUtilities.invokeLater(() -> progressBar.setMaximum(total));

                for (int i = 0; i < total; i++) {
                    ProxyHttpRequestResponse item = history.get(i);
                    handleExport(item.request(), item.response(), "hist_");
                    final int p = i + 1;
                    SwingUtilities.invokeLater(() -> progressBar.setValue(p));
                }
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(mainPanel, "History Export Complete!");
                });
            } catch (Exception e) { api.logging().logToError("History export error: " + e.getMessage()); }
        }).start();
    }

    // Helper to parse comma separated values
    private List<String> getCsvList(String text) {
        List<String> list = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return list;
        for (String s : text.split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim().toLowerCase());
        }
        return list;
    }

    public synchronized void handleExport(HttpRequest req, HttpResponse res, String prefix) {
        if (req == null || currentSessionDir.isEmpty()) return;
        
        // 1. Scope Filter
        if (scopeCheck.isSelected() && !api.scope().isInScope(req.url())) return;
        
        // 2. Extension Filter
        String reqPath = req.pathWithoutQuery().toLowerCase();
        for (String ext : getCsvList(excludeExtField.getText())) {
            if (reqPath.endsWith(ext)) return;
        }

        String host = req.httpService() != null ? req.httpService().host().toLowerCase() : "";

        // 3. Include Hosts Filter
        List<String> includeHosts = getCsvList(includeHostsField.getText());
        if (!includeHosts.isEmpty()) {
            boolean matchesInc = false;
            for (String inc : includeHosts) {
                if (host.contains(inc)) { matchesInc = true; break; }
            }
            if (!matchesInc) return;
        }

        // 4. Exclude Hosts Filter
        List<String> excludeHosts = getCsvList(excludeHostsField.getText());
        for (String exc : excludeHosts) {
            if (host.contains(exc)) return;
        }

        // 5. HTTP Methods Filter
        List<String> methods = getCsvList(methodsField.getText());
        if (!methods.isEmpty()) {
            boolean matchesMethod = false;
            for (String m : methods) {
                if (req.method().equalsIgnoreCase(m)) { matchesMethod = true; break; }
            }
            if (!matchesMethod) return;
        }

        // 6. Status Codes Filter
        if (res != null) {
            List<String> statusCodes = getCsvList(statusCodesField.getText());
            if (!statusCodes.isEmpty()) {
                boolean matchesStatus = false;
                String resStatus = String.valueOf(res.statusCode());
                for (String code : statusCodes) {
                    // Support exact match or wildcard like 4xx, 5xx
                    code = code.replace("x", ""); 
                    if (resStatus.startsWith(code)) { matchesStatus = true; break; }
                }
                if (!matchesStatus) return;
            }
        }

        // 7. Deduplication Filter
        if (dedupeCheck.isSelected() && dedupeFilter.isDuplicate(req, res)) return;

        // Passed all filters! Process the export
        fileCounter++;
        sessionExportCount++;
        saveFilesAndLog(req, res, currentSessionDir, prefix + fileCounter);
        updateStats();
    }

    private void saveFilesAndLog(HttpRequest req, HttpResponse res, String sessionDir, String filePrefix) {
        new Thread(() -> {
            try {
                if (req == null) return;
                if (req.httpService() == null) {
                    api.logging().logToError("Export failed: HTTP Service is null for " + req.url());
                    return;
                }

                String host = req.httpService().host();
                String hostSanitized = host.replaceAll("[^a-zA-Z0-9.-]", "_");
                
                File hostDataDir = new File(sessionDir, "targets/" + hostSanitized + "/data");
                if (!hostDataDir.exists()) hostDataDir.mkdirs();

                String reqName = filePrefix + "_req.txt";
                String resName = filePrefix + "_res.txt";

                // Save Raw Files
                File reqFile = new File(hostDataDir, reqName);
                try (FileOutputStream fos = new FileOutputStream(reqFile)) {
                    fos.write(req.toByteArray().getBytes());
                }

                if (res != null && res.toByteArray() != null) {
                    File resFile = new File(hostDataDir, resName);
                    try (FileOutputStream fos = new FileOutputStream(resFile)) {
                        fos.write(res.toByteArray().getBytes());
                    }
                }

                // Update Maps
                if (mapCheck.isSelected()) {
                    String time = timeFormat.format(new Date());
                    String relReqPath = "targets/" + hostSanitized + "/data/" + reqName;
                    String relResPath = res != null ? "targets/" + hostSanitized + "/data/" + resName : "N/A";
                    mapGenerator.addEntry(req.url(), host, req.method(), res != null ? res.statusCode() : 0, relReqPath, relResPath, time, sessionDir);
                }

                // Update UI Table synchronously
                SwingUtilities.invokeLater(() -> {
                    loggedRequests.add(req);
                    tableModel.addRow(new Object[]{
                        filePrefix, timeFormat.format(new Date()), req.method(), host, 
                        req.pathWithoutQuery(), res != null ? res.statusCode() : "N/A", "targets/" + hostSanitized
                    });
                    logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount() - 1, 0, true));
                });
            } catch (Exception e) { 
                api.logging().logToError("Real-time write error for " + (req != null ? req.url() : "unknown") + ": " + e.getMessage()); 
            }
        }).start();
    }
    
    public void manualExportFromContext(HttpRequest req) {
        if (req != null) {
            handleExport(req, null, "manual_");
            api.logging().logToOutput("Manually logged: " + req.url());
        }
    }

    public JPanel getPanel() { return mainPanel; }
    public boolean isRealTimeEnabled() { return enableLoggingCheck.isSelected(); }
    public String getSessionDir() { return currentSessionDir; }
}
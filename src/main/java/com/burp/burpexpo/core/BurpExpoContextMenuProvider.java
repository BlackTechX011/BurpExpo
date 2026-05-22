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
 
package com.burp.burpexpo.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.burp.burpexpo.ui.TrafficExporterUI;
import com.burp.burpexpo.utils.SnippetGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class BurpExpoContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final TrafficExporterUI ui;
    private final String[] supportedLanguages = {
        "cURL", "Wget", "Python Request", "PHP HTTP_Request2", 
        "Go Native", "NodeJS Request", "PowerShell", "Javascript XHR"
    };

    public BurpExpoContextMenuProvider(MontoyaApi api, TrafficExporterUI ui) {
        this.api = api;
        this.ui = ui;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuList = new ArrayList<>();
        
        HttpRequest request = null;
        if (event.messageEditorRequestResponse().isPresent()) {
            request = event.messageEditorRequestResponse().get().requestResponse().request();
        } else if (!event.selectedRequestResponses().isEmpty()) {
            request = event.selectedRequestResponses().get(0).request();
        }

        if (request != null) {
            JMenu parentMenu = new JMenu("BurpExpo");
            JMenu clipboardMenu = new JMenu("To Clipboard");
            JMenu fileMenu = new JMenu("To File");

            HttpRequest finalReq = request;

            for (String lang : supportedLanguages) {
                clipboardMenu.add(createClipboardItem(lang, finalReq));
                fileMenu.add(createFileItem(lang, finalReq));
            }

            JMenuItem sendToExporter = new JMenuItem("Send to BurpExpo Logging Session");
            sendToExporter.addActionListener(e -> ui.manualExportFromContext(finalReq));

            parentMenu.add(clipboardMenu);
            parentMenu.add(fileMenu);
            parentMenu.addSeparator();
            parentMenu.add(sendToExporter);
            
            menuList.add(parentMenu);
        }
        return menuList;
    }

    private JMenuItem createClipboardItem(String language, HttpRequest request) {
        JMenuItem item = new JMenuItem(language);
        item.addActionListener(e -> {
            String code = SnippetGenerator.generateCode(request, language);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        });
        return item;
    }

    private JMenuItem createFileItem(String language, HttpRequest request) {
        JMenuItem item = new JMenuItem(language);
        item.addActionListener(e -> {
            String code = SnippetGenerator.generateCode(request, language);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save " + language + " Snippet");
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    Files.writeString(file.toPath(), code, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (Exception ex) {
                    api.logging().logToError("Failed to save snippet: " + ex.getMessage());
                }
            }
        });
        return item;
    }
}
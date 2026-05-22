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
 
package com.burp.burpexpo;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.burp.burpexpo.core.BurpExpoContextMenuProvider;
import com.burp.burpexpo.core.TrafficExporterHttpHandler;
import com.burp.burpexpo.ui.TrafficExporterUI;

public class MainExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("BurpExpo");

        // Initialize the UI
        TrafficExporterUI ui = new TrafficExporterUI(api);
        api.userInterface().registerSuiteTab("BurpExpo", ui.getPanel());

        // Register the HTTP Handler for real-time traffic
        api.http().registerHttpHandler(new TrafficExporterHttpHandler(api, ui));

        // Register the Context Menu Provider (Right-Click features anywhere in Burp)
        api.userInterface().registerContextMenuItemsProvider(new BurpExpoContextMenuProvider(api, ui));

        api.logging().logToOutput("BurpExpo loaded successfully. ");
    }
}
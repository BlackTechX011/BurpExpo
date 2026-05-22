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
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burp.burpexpo.ui.TrafficExporterUI;

public class TrafficExporterHttpHandler implements HttpHandler {
    private final MontoyaApi api;
    private final TrafficExporterUI ui;

    public TrafficExporterHttpHandler(MontoyaApi api, TrafficExporterUI ui) {
        this.api = api;
        this.ui = ui;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // 1. Check if Real-Time Export is enabled
        if (!ui.isRealTimeEnabled()) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        // 2. Pass to centralized export handler where all Filters (Hosts, Ext, Codes) are processed
        ui.handleExport(responseReceived.initiatingRequest(), responseReceived, "rt_");

        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
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
 
package com.burp.burpexpo.filter;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DeduplicationFilter {
    private final Set<String> seenHashes = Collections.synchronizedSet(new HashSet<>());

    public boolean isDuplicate(HttpRequest request, HttpResponse response) {
        try {
            String key = request.method() + request.url() + (response != null ? response.statusCode() : "0");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(key.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String hash = sb.toString();

            if (seenHashes.contains(hash)) return true;
            seenHashes.add(hash);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void clear() {
        seenHashes.clear();
    }
}
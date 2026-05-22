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
 
package com.burp.burpexpo.io;

import burp.api.montoya.logging.Logging;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapGenerator {
    private final Logging logging;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<MapEntry> globalEntries = new ArrayList<>();
    private final Map<String, List<MapEntry>> targetEntries = new HashMap<>();

    public MapGenerator(Logging logging) {
        this.logging = logging;
    }

    public void addEntry(String url, String host, String method, int status, String reqFile, String resFile, String time, String sessionDir) {
        lock.writeLock().lock();
        try {
            MapEntry entry = new MapEntry(url, host, method, status, reqFile, resFile, time);
            globalEntries.add(entry);
            
            targetEntries.computeIfAbsent(host, k -> new ArrayList<>()).add(entry);
            
            updateGlobalFiles(sessionDir);
            updateTargetFiles(sessionDir, host);
        } catch (IOException e) {
            if (logging != null) logging.logToError("Map update failed: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateGlobalFiles(String sessionDir) throws IOException {
        Path targetPath = Path.of(sessionDir);
        generateFiles(targetPath, globalEntries, "Global AI Traffic Map");
    }

    private void updateTargetFiles(String sessionDir, String host) throws IOException {
        Path hostPath = Path.of(sessionDir, "targets", sanitize(host));
        if (!Files.exists(hostPath)) Files.createDirectories(hostPath);
        
        generateFiles(hostPath, targetEntries.get(host), "AI Traffic Map for " + host);
    }

    private void generateFiles(Path path, List<MapEntry> entries, String title) throws IOException {
        Path jsonPath = path.resolve("map.json");
        Path txtPath = path.resolve("map.txt");

        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            MapEntry entry = entries.get(i);
            json.append(String.format("  {\n    \"time\": \"%s\",\n    \"url\": \"%s\",\n    \"host\": \"%s\",\n    \"method\": \"%s\",\n    \"status\": %d,\n    \"request_file\": \"%s\",\n    \"response_file\": \"%s\"\n  }",
                    entry.time, entry.url, entry.host, entry.method, entry.status, entry.reqFile, entry.resFile));
            if (i < entries.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("\n]");

        StringBuilder txt = new StringBuilder(title + "\n" + "=".repeat(title.length()) + "\n\n");
        for (MapEntry entry : entries) {
            txt.append(String.format("[%s] [%d] %s %s\n    Req: %s | Res: %s\n\n",
                    entry.time, entry.status, entry.method, entry.url, entry.reqFile, entry.resFile));
        }

        Files.writeString(jsonPath, json.toString(), StandardCharsets.UTF_8);
        Files.writeString(txtPath, txt.toString(), StandardCharsets.UTF_8);
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static class MapEntry {
        String url, host, method, reqFile, resFile, time;
        int status;

        MapEntry(String url, String host, String method, int status, String reqFile, String resFile, String time) {
            this.url = url;
            this.host = host;
            this.method = method;
            this.status = status;
            this.reqFile = reqFile;
            this.resFile = resFile;
            this.time = time;
        }
    }
}
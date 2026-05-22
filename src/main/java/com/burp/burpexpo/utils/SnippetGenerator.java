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

package com.burp.burpexpo.utils;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;

public class SnippetGenerator {

    public static String generateCode(HttpRequest request, String language) {
        switch (language) {
            case "cURL": return generateCurl(request);
            case "Python Request": return generatePython(request);
            case "Wget": return generateWget(request);
            case "PowerShell": return generatePowerShell(request);
            case "Go Native": return generateGo(request);
            case "NodeJS Request": return generateNode(request);
            case "PHP HTTP_Request2": return generatePhp(request);
            case "Javascript XHR": return generateXhr(request);
            default: return "Language not supported yet.";
        }
    }

    private static String generateCurl(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl -i -s -k -X '").append(req.method()).append("' \\\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host")) continue;
            sb.append("    -H '").append(h.name()).append(": ").append(h.value().replace("'", "'\\''")).append("' \\\n");
        }
        if (req.body().length() > 0) {
            sb.append("    --data-binary '").append(req.bodyToString().replace("'", "'\\''")).append("' \\\n");
        }
        sb.append("    '").append(req.url()).append("'");
        return sb.toString();
    }

    private static String generatePython(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("import requests\n");
        sb.append("requests.packages.urllib3.disable_warnings()\n\n");
        sb.append("url = \"").append(req.url()).append("\"\n\n");
        
        sb.append("headers = {\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("    \"").append(h.name()).append("\": \"").append(h.value().replace("\"", "\\\"")).append("\",\n");
        }
        sb.append("}\n\n");

        if (req.body().length() > 0) {
            sb.append("payload = \"\"\"").append(req.bodyToString()).append("\"\"\"\n");
            sb.append("response = requests.request(\"").append(req.method()).append("\", url, headers=headers, data=payload, verify=False)\n");
        } else {
            sb.append("response = requests.request(\"").append(req.method()).append("\", url, headers=headers, verify=False)\n");
        }
        sb.append("print(response.text)\n");
        return sb.toString();
    }

    private static String generateWget(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("wget --no-check-certificate --quiet -S \\\n");
        sb.append("  --method '").append(req.method()).append("' \\\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("  --header '").append(h.name()).append(": ").append(h.value().replace("'", "'\\''")).append("' \\\n");
        }
        if (req.body().length() > 0) {
            sb.append("  --body-data '").append(req.bodyToString().replace("'", "'\\''")).append("' \\\n");
        }
        sb.append("  '").append(req.url()).append("'");
        return sb.toString();
    }

    private static String generatePowerShell(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("$headers = New-Object \"System.Collections.Generic.Dictionary[[String],[String]]\"\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("$headers.Add(\"").append(h.name()).append("\", \"").append(h.value().replace("\"", "`\"")).append("\")\n");
        }
        sb.append("\n$url = \"").append(req.url()).append("\"\n");
        
        if (req.body().length() > 0) {
            sb.append("$body = @\"\n").append(req.bodyToString()).append("\n\"@\n");
            sb.append("$response = Invoke-RestMethod -Uri $url -Method '").append(req.method()).append("' -Headers $headers -Body $body\n");
        } else {
            sb.append("$response = Invoke-RestMethod -Uri $url -Method '").append(req.method()).append("' -Headers $headers\n");
        }
        sb.append("$response | ConvertTo-Json\n");
        return sb.toString();
    }
    
    private static String generateGo(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("package main\n\nimport (\n  \"fmt\"\n  \"strings\"\n  \"net/http\"\n  \"io/ioutil\"\n)\n\nfunc main() {\n");
        sb.append("  url := \"").append(req.url()).append("\"\n");
        sb.append("  method := \"").append(req.method()).append("\"\n");
        if (req.body().length() > 0) {
            sb.append("  payload := strings.NewReader(`").append(req.bodyToString()).append("`)\n");
        } else {
            sb.append("  payload := strings.NewReader(\"\")\n");
        }
        sb.append("  client := &http.Client{}\n");
        sb.append("  req, err := http.NewRequest(method, url, payload)\n  if err != nil {\n    fmt.Println(err)\n    return\n  }\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("  req.Header.Add(\"").append(h.name()).append("\", \"").append(h.value().replace("\"", "\\\"")).append("\")\n");
        }
        sb.append("  res, err := client.Do(req)\n  if err != nil {\n    fmt.Println(err)\n    return\n  }\n  defer res.Body.Close()\n");
        sb.append("  body, err := ioutil.ReadAll(res.Body)\n  fmt.Println(string(body))\n}\n");
        return sb.toString();
    }

    private static String generateNode(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("var request = require('request');\nvar options = {\n");
        sb.append("  'method': '").append(req.method()).append("',\n");
        sb.append("  'url': '").append(req.url()).append("',\n");
        sb.append("  'headers': {\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("    '").append(h.name()).append("': '").append(h.value().replace("'", "\\'")).append("',\n");
        }
        sb.append("  },\n");
        if (req.body().length() > 0) {
            sb.append("  body: `").append(req.bodyToString()).append("`\n");
        }
        sb.append("};\nrequest(options, function (error, response) {\n  if (error) throw new Error(error);\n  console.log(response.body);\n});\n");
        return sb.toString();
    }

    private static String generatePhp(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?php\n$curl = curl_init();\ncurl_setopt_array($curl, array(\n");
        sb.append("  CURLOPT_URL => '").append(req.url()).append("',\n");
        sb.append("  CURLOPT_RETURNTRANSFER => true,\n");
        sb.append("  CURLOPT_CUSTOMREQUEST => '").append(req.method()).append("',\n");
        if (req.body().length() > 0) {
            sb.append("  CURLOPT_POSTFIELDS => '").append(req.bodyToString().replace("'", "\\'")).append("',\n");
        }
        sb.append("  CURLOPT_HTTPHEADER => array(\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("    '").append(h.name()).append(": ").append(h.value().replace("'", "\\'")).append("',\n");
        }
        sb.append("  ),\n));\n");
        sb.append("$response = curl_exec($curl);\ncurl_close($curl);\necho $response;\n");
        return sb.toString();
    }

    private static String generateXhr(HttpRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("var xhr = new XMLHttpRequest();\n");
        sb.append("xhr.withCredentials = true;\n");
        sb.append("xhr.addEventListener(\"readystatechange\", function() {\n");
        sb.append("  if(this.readyState === 4) {\n    console.log(this.responseText);\n  }\n});\n\n");
        sb.append("xhr.open(\"").append(req.method()).append("\", \"").append(req.url()).append("\");\n");
        for (HttpHeader h : req.headers()) {
            if (h.name().equalsIgnoreCase("Host") || h.name().equalsIgnoreCase("Content-Length")) continue;
            sb.append("xhr.setRequestHeader(\"").append(h.name()).append("\", \"").append(h.value().replace("\"", "\\\"")).append("\");\n");
        }
        if (req.body().length() > 0) {
            sb.append("\nvar data = `").append(req.bodyToString()).append("`;\n");
            sb.append("xhr.send(data);\n");
        } else {
            sb.append("xhr.send();\n");
        }
        return sb.toString();
    }
}
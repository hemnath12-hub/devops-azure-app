package com.devops.vault.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@CrossOrigin(origins = "*")
public class SystemInfoController {

    private final Environment environment;

    @Value("${BUILD_NUMBER:104}")
    private String buildNumber;

    @Value("${GIT_COMMIT:a8f23bc}")
    private String gitCommit;

    @Value("${TARGET_ENV:Development}")
    private String targetEnv;

    @Value("${azure.storage.enabled:false}")
    private boolean azureStorageEnabled;

    public SystemInfoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "Secure Asset & Document Vault");
        info.put("version", "1.0.0");
        info.put("buildNumber", buildNumber);
        info.put("gitCommit", gitCommit);
        info.put("environment", targetEnv);
        info.put("azureStorageEnabled", azureStorageEnabled);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        info.put("activeProfiles", environment.getActiveProfiles());

        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        info.put("usedMemoryMb", (totalMem - freeMem) / (1024 * 1024));
        info.put("maxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));

        return ResponseEntity.ok(info);
    }
}

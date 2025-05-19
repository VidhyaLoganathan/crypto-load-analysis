package com.cypher.cardload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints and API information
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final Environment environment;
    private final ApplicationContext applicationContext;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "cypher-load-analytics");
        response.put("timestamp", System.currentTimeMillis());

        // Add simulator mode info
        boolean simulatorMode = Boolean.parseBoolean(
                environment.getProperty("load-simulator.enabled", "false"));
        response.put("simulator_mode", simulatorMode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Cypher Load Analytics API");
        response.put("version", "1.0.0");
        response.put("description", "API for Cypher crypto card load analytics");

        // Add simulator mode info
        boolean simulatorMode = Boolean.parseBoolean(
                environment.getProperty("load-simulator.enabled", "false"));
        response.put("simulator_mode", simulatorMode);

        // Add endpoints info
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /api/load-volume/daily", "Daily load volume");
        endpoints.put("GET /api/load-volume/weekly", "Weekly load volume");
        endpoints.put("GET /api/load-volume/monthly", "Monthly load volume");
        endpoints.put("GET /api/load-volume/summary", "Summary load volume");
        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("simulator_enabled", Boolean.parseBoolean(
                environment.getProperty("load-simulator.enabled", "false")));

        // Get all bean names
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        List<String> relevantBeans = Arrays.stream(beanNames)
                .filter(name -> name.contains("LoadVolume") || name.contains("Simulator"))
                .collect(Collectors.toList());

        response.put("active_services", relevantBeans);

        return ResponseEntity.ok(response);
    }
}
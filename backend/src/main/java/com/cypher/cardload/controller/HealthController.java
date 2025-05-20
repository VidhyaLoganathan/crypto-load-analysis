package com.cypher.cardload.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cypher.cardload.service.TokenPriceService;
import com.cypher.cardload.service.PriceService;
import com.cypher.cardload.service.ContractDetectionService;
import com.cypher.cardload.service.WalletAnalysisService;
import com.cypher.cardload.service.SimulatedLoadVolumeService;
import com.cypher.cardload.service.TransactionCacheService;
import com.cypher.cardload.service.LoadVolumeService;
import com.cypher.cardload.service.DataSyncService;
import com.cypher.cardload.service.LoadSimulatorService;
import com.cypher.cardload.service.TransactionService;
import com.cypher.cardload.service.BlockchainService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health, Info, and Debug endpoints for Cypher Load Analytics API.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private static final Map<String, String> ENDPOINTS = createEndpoints();

    private final Environment environment;
    private final ApplicationContext applicationContext;

    // Injected Services
    private final TokenPriceService tokenPriceService;
    private final PriceService priceService;
    private final ContractDetectionService contractDetectionService;
    private final WalletAnalysisService walletAnalysisService;
    private final SimulatedLoadVolumeService simulatedLoadVolumeService;
    private final TransactionCacheService transactionCacheService;
    private final LoadVolumeService loadVolumeService;
    private final DataSyncService dataSyncService;
    private final LoadSimulatorService loadSimulatorService;
    private final TransactionService transactionService;
    private final BlockchainService blockchainService;


    private static Map<String, String> createEndpoints() {
        Map<String, String> endpoints = new HashMap<>();

        // Load‐volume endpoints
        endpoints.put("GET /api/load-volume/daily", "Daily load volume");
        endpoints.put("GET /api/load-volume/weekly", "Weekly load volume");
        endpoints.put("GET /api/load-volume/monthly", "Monthly load volume");
        endpoints.put("GET /api/load-volume/summary", "Summary load volume");

        // Wallet‐analysis endpoints
        endpoints.put("POST /api/v1/wallet/analyze", "Analyze wallet (body: WalletAnalysisRequest)");
        endpoints.put("GET  /api/v1/wallet/analyze/{address}", "Get wallet analysis for address");

        return endpoints;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "UP");
        resp.put("service", "cypher-load-analytics");
        resp.put("timestamp", System.currentTimeMillis());
        resp.put("simulator_mode", isSimulatorMode());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("name", "Cypher Load Analytics API");
        resp.put("version", "1.0.0");
        resp.put("description", "API for Cypher crypto card load analytics");
        resp.put("simulator_mode", isSimulatorMode());
        resp.put("endpoints", ENDPOINTS);
        return ResponseEntity.ok(resp);
    }


    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugInfo() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("simulator_enabled", isSimulatorMode());

        String[] allBeans = applicationContext.getBeanDefinitionNames();
        List<String> serviceBeans = Arrays.stream(allBeans)
                .filter(name -> name.endsWith("Service"))
                .collect(Collectors.toList());

        resp.put("active_services", serviceBeans);
        return ResponseEntity.ok(resp);
    }

    private boolean isSimulatorMode() {
        return Boolean.parseBoolean(
                environment.getProperty("load-simulator.enabled", "false")
        );
    }
}

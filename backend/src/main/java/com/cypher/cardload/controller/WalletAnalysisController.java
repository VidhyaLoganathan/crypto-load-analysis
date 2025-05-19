package com.cypher.cardload.controller;

import com.cypher.cardload.model.WalletAnalysisRequest;
import com.cypher.cardload.model.WalletAnalysisResponse;
import com.cypher.cardload.service.WalletAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletAnalysisController {
    private final WalletAnalysisService walletAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<WalletAnalysisResponse> analyzeWallet(@RequestBody WalletAnalysisRequest request) {
        log.info("Received wallet analysis request for: {}", request.getWalletAddress());
        WalletAnalysisResponse response = walletAnalysisService.analyzeWallet(
                request.getWalletAddress(),
                request.getLimit());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyze/{address}")
    public ResponseEntity<WalletAnalysisResponse> getWalletAnalysis(
            @PathVariable String address,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Received wallet analysis GET request for: {}", address);
        WalletAnalysisResponse response = walletAnalysisService.analyzeWallet(address, limit);
        return ResponseEntity.ok(response);
    }
}
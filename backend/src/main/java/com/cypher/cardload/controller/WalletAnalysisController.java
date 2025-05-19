package com.cypher.cardload.controller;

import com.cypher.cardload.model.WalletAnalysisRequest;
import com.cypher.cardload.model.WalletAnalysisResponse;
import com.cypher.cardload.service.WalletAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WalletAnalysisController {
    private final WalletAnalysisService walletAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<WalletAnalysisResponse> analyzeWallet(
            @Valid @RequestBody WalletAnalysisRequest request) {
        log.info("Received wallet analysis request for: {}", request.getWalletAddress());
        WalletAnalysisResponse response = walletAnalysisService.analyzeWallet(
                request.getWalletAddress(),
                request.getLimit());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyze/{address}")
    public ResponseEntity<WalletAnalysisResponse> getWalletAnalysis(
            @PathVariable
            @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address format")
            String address,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Received wallet analysis GET request for: {}", address);
        WalletAnalysisResponse response = walletAnalysisService.analyzeWallet(address, limit);
        return ResponseEntity.ok(response);
    }
}

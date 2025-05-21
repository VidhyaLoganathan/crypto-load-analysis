package com.cypher.cardload.controller;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.BlockchainService;
import com.cypher.cardload.service.LoadVolumeService;
import com.cypher.cardload.simulator.SimulatedLoadVolumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * A controller that automatically routes to either real or simulated data
 * based on settings and blockchain availability.
 */
@RestController
@RequestMapping("/api/load-volume")
@Slf4j
public class SmartLoadVolumeController {

    @Value("${load-simulator.enabled:true}")
    private boolean simulatorEnabled;

    private final LoadVolumeService loadVolumeService;
    private final SimulatedLoadVolumeService simulatedLoadVolumeService;
    private final BlockchainService blockchainService;

    public SmartLoadVolumeController(
            LoadVolumeService loadVolumeService,
            SimulatedLoadVolumeService simulatedLoadVolumeService,
            BlockchainService blockchainService) {
        this.loadVolumeService = loadVolumeService;
        this.simulatedLoadVolumeService = simulatedLoadVolumeService;
        this.blockchainService = blockchainService;

        log.info("SmartLoadVolumeController initialized with simulatorEnabled={}", simulatorEnabled);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<LoadVolumeData>> getDailyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting daily load volume from {} to {}", startDate, endDate);

        // Force simulator mode if it's explicitly enabled
        if (simulatorEnabled) {
            log.info("Using simulator (forced by config)");
            return ResponseEntity.ok(simulatedLoadVolumeService.getDailyLoadVolume(startDate, endDate));
        }

        // Try to test blockchain connection
        try {
            blockchainService.getCurrentBlockNumber();
            log.info("Using real blockchain data");
            return ResponseEntity.ok(loadVolumeService.getDailyLoadVolume(startDate, endDate));
        } catch (Exception e) {
            // Fall back to simulator if blockchain connection fails
            log.warn("Blockchain connection failed, falling back to simulator: {}", e.getMessage());
            return ResponseEntity.ok(simulatedLoadVolumeService.getDailyLoadVolume(startDate, endDate));
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<LoadVolumeData>> getWeeklyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting weekly load volume from {} to {}", startDate, endDate);

        // Force simulator mode if it's explicitly enabled
        if (simulatorEnabled) {
            log.info("Using simulator (forced by config)");
            return ResponseEntity.ok(simulatedLoadVolumeService.getWeeklyLoadVolume(startDate, endDate));
        }

        // Try to test blockchain connection
        try {
            blockchainService.getCurrentBlockNumber();
            log.info("Using real blockchain data");
            return ResponseEntity.ok(loadVolumeService.getWeeklyLoadVolume(startDate, endDate));
        } catch (Exception e) {
            // Fall back to simulator if blockchain connection fails
            log.warn("Blockchain connection failed, falling back to simulator: {}", e.getMessage());
            return ResponseEntity.ok(simulatedLoadVolumeService.getWeeklyLoadVolume(startDate, endDate));
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<LoadVolumeData>> getMonthlyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting monthly load volume from {} to {}", startDate, endDate);

        // Force simulator mode if it's explicitly enabled
        if (simulatorEnabled) {
            log.info("Using simulator (forced by config)");
            return ResponseEntity.ok(simulatedLoadVolumeService.getMonthlyLoadVolume(startDate, endDate));
        }

        // Try to test blockchain connection
        try {
            blockchainService.getCurrentBlockNumber();
            log.info("Using real blockchain data");
            return ResponseEntity.ok(loadVolumeService.getMonthlyLoadVolume(startDate, endDate));
        } catch (Exception e) {
            // Fall back to simulator if blockchain connection fails
            log.warn("Blockchain connection failed, falling back to simulator: {}", e.getMessage());
            return ResponseEntity.ok(simulatedLoadVolumeService.getMonthlyLoadVolume(startDate, endDate));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<LoadVolumeData> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting summary load volume from {} to {}", startDate, endDate);

        // Force simulator mode if it's explicitly enabled
        if (simulatorEnabled) {
            log.info("Using simulator (forced by config)");
            return ResponseEntity.ok(simulatedLoadVolumeService.getSummary(startDate, endDate));
        }

        // Try to test blockchain connection
        try {
            blockchainService.getCurrentBlockNumber();
            log.info("Using real blockchain data");
            return ResponseEntity.ok(loadVolumeService.getSummary(startDate, endDate));
        } catch (Exception e) {
            // Fall back to simulator if blockchain connection fails
            log.warn("Blockchain connection failed, falling back to simulator: {}", e.getMessage());
            return ResponseEntity.ok(simulatedLoadVolumeService.getSummary(startDate, endDate));
        }
    }
}

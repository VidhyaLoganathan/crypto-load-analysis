
package com.cypher.cardload.controller;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.LoadVolumeService;
import com.cypher.cardload.service.SimulatedLoadVolumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/load-volume")
@Slf4j
public class LoadVolumeController {

    @Value("${load-simulator.enabled:false}")
    private boolean simulatorEnabled;

    private final LoadVolumeService loadVolumeService;
    private final SimulatedLoadVolumeService simulatedLoadVolumeService;

    @Autowired
    public LoadVolumeController(
            @Autowired(required = false) LoadVolumeService loadVolumeService,
            @Autowired(required = false) SimulatedLoadVolumeService simulatedLoadVolumeService) {
        this.loadVolumeService = loadVolumeService;
        this.simulatedLoadVolumeService = simulatedLoadVolumeService;

        log.info("LoadVolumeController initialized with simulatorEnabled={}", simulatorEnabled);
        log.info("Real service available: {}", loadVolumeService != null);
        log.info("Simulator service available: {}", simulatedLoadVolumeService != null);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<LoadVolumeData>> getDailyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting daily load volume from {} to {} (simulator: {})", startDate, endDate, simulatorEnabled);

        if (simulatorEnabled && simulatedLoadVolumeService != null) {
            log.info("Using simulator service");
            return ResponseEntity.ok(simulatedLoadVolumeService.getDailyLoadVolume(startDate, endDate));
        } else if (loadVolumeService != null) {
            log.info("Using real service");
            return ResponseEntity.ok(loadVolumeService.getDailyLoadVolume(startDate, endDate));
        } else {
            log.error("No service available!");
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<LoadVolumeData>> getWeeklyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting weekly load volume from {} to {} (simulator: {})", startDate, endDate, simulatorEnabled);

        if (simulatorEnabled && simulatedLoadVolumeService != null) {
            return ResponseEntity.ok(simulatedLoadVolumeService.getWeeklyLoadVolume(startDate, endDate));
        } else if (loadVolumeService != null) {
            return ResponseEntity.ok(loadVolumeService.getWeeklyLoadVolume(startDate, endDate));
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<LoadVolumeData>> getMonthlyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting monthly load volume from {} to {} (simulator: {})", startDate, endDate, simulatorEnabled);

        if (simulatorEnabled && simulatedLoadVolumeService != null) {
            return ResponseEntity.ok(simulatedLoadVolumeService.getMonthlyLoadVolume(startDate, endDate));
        } else if (loadVolumeService != null) {
            return ResponseEntity.ok(loadVolumeService.getMonthlyLoadVolume(startDate, endDate));
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<LoadVolumeData> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting summary load volume from {} to {} (simulator: {})", startDate, endDate, simulatorEnabled);

        if (simulatorEnabled && simulatedLoadVolumeService != null) {
            return ResponseEntity.ok(simulatedLoadVolumeService.getSummary(startDate, endDate));
        } else if (loadVolumeService != null) {
            return ResponseEntity.ok(loadVolumeService.getSummary(startDate, endDate));
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
}

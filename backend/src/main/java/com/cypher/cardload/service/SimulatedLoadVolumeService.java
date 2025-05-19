package com.cypher.cardload.service;

import com.cypher.cardload.model.LoadVolumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * This service uses the simulator to provide load volume data when blockchain RPC is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulatedLoadVolumeService {

    private final LoadSimulatorService loadSimulatorService;

    public List<LoadVolumeData> getDailyLoadVolume(LocalDate startDate, LocalDate endDate) {
        log.info("Using simulated data for daily load volume from {} to {}", startDate, endDate);
        return loadSimulatorService.generateDailyLoadVolumeData(startDate, endDate);
    }

    public List<LoadVolumeData> getWeeklyLoadVolume(LocalDate startDate, LocalDate endDate) {
        log.info("Using simulated data for weekly load volume from {} to {}", startDate, endDate);
        return loadSimulatorService.generateWeeklyLoadVolumeData(startDate, endDate);
    }

    public List<LoadVolumeData> getMonthlyLoadVolume(LocalDate startDate, LocalDate endDate) {
        log.info("Using simulated data for monthly load volume from {} to {}", startDate, endDate);
        return loadSimulatorService.generateMonthlyLoadVolumeData(startDate, endDate);
    }

    public LoadVolumeData getSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Using simulated data for summary load volume from {} to {}", startDate, endDate);
        return loadSimulatorService.generateSummaryData(startDate, endDate);
    }
}

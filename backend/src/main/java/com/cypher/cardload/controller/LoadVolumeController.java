
package com.cypher.cardload.controller;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.LoadVolumeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.LoadVolumeService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class LoadVolumeController {

    private final LoadVolumeService loadVolumeService;

    @GetMapping("/daily")
    public ResponseEntity<List<LoadVolumeData>> getDailyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(loadVolumeService.getDailyLoadVolume(startDate, endDate));
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<LoadVolumeData>> getWeeklyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(loadVolumeService.getWeeklyLoadVolume(startDate, endDate));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<LoadVolumeData>> getMonthlyLoadVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(loadVolumeService.getMonthlyLoadVolume(startDate, endDate));
    }

    @GetMapping("/summary")
    public ResponseEntity<LoadVolumeData> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(loadVolumeService.getSummary(startDate, endDate));
    }
}

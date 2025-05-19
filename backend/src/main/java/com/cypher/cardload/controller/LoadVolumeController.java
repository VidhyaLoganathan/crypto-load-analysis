
package com.cypher.cardload.controller;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.LoadVolumeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LoadVolumeController {
    private final LoadVolumeService service;

    public LoadVolumeController(LoadVolumeService service) {
        this.service = service;
    }

    @GetMapping("/volume")
    public List<LoadVolumeData> getVolume(@RequestParam(defaultValue="daily") String period) {
        return service.getVolume(period);
    }
}

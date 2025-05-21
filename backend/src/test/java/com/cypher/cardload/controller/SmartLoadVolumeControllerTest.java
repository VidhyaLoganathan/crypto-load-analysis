package com.cypher.cardload.controller;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.service.BlockchainService;
import com.cypher.cardload.service.LoadVolumeService;
import com.cypher.cardload.simulator.SimulatedLoadVolumeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SmartLoadVolumeController.class)
@TestPropertySource(properties = "load-simulator.enabled=true")
class SmartLoadVolumeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LoadVolumeService loadVolumeService;

    @MockBean
    private SimulatedLoadVolumeService simulatedLoadVolumeService;

    @MockBean
    private BlockchainService blockchainService;

    @Test
    void dailyEndpoint_returnsSimulatorData() throws Exception {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 1, 2);

        var sample = LoadVolumeData.builder()
                .date(start)
                .totalUsdValue(BigDecimal.valueOf(123))
                .tokenBreakdown(Map.of("ETH", BigDecimal.valueOf(123)))
                .build();

        Mockito.when(simulatedLoadVolumeService.getDailyLoadVolume(start, end))
                .thenReturn(List.of(sample));

        mvc.perform(get("/api/load-volume/daily")
                        .param("startDate", start.toString())
                        .param("endDate",   end.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value(start.toString()))
                .andExpect(jsonPath("$[0].totalUsdValue").value(123));
    }
}

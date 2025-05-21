package com.cypher.cardload.controller;

import com.cypher.cardload.model.CounterpartyInfo;
import com.cypher.cardload.model.CounterpartyType;
import com.cypher.cardload.dto.WalletAnalysisRequest;
import com.cypher.cardload.dto.WalletAnalysisResponse;
import com.cypher.cardload.service.WalletAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletAnalysisController.class)
class WalletAnalysisControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private WalletAnalysisService analysisService;

    private static final String VALID_ADDR = "0x0123456789abcdef0123456789abcdef01234567";

    private static WalletAnalysisResponse sampleResponse() {
        CounterpartyInfo cp = CounterpartyInfo.builder()
                .address("0xAAAAbbbbCCCCddddEEEEffff0000111122223333")
                .transactionCount(42)
                .name("Example Protocol")
                .type(CounterpartyType.PROTOCOL)
                .protocol("ExampleProtocol")
                .isKnownEntity(true)
                .etherscanUrl("https://etherscan.io/address/0xAAAAbbbbCCCCddddEEEEffff0000111122223333")
                .build();

        return WalletAnalysisResponse.builder()
                .walletAddress(VALID_ADDR)
                .totalTransactions(5)
                .topCounterparties(List.of(cp))
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/wallet/analyze")
    class PostAnalyze {

        @Test
        @DisplayName("200 and correct JSON on valid request")
        void validRequest() throws Exception {
            WalletAnalysisRequest req = new WalletAnalysisRequest();
            req.setWalletAddress(VALID_ADDR);
            req.setLimit(3);

            WalletAnalysisResponse resp = sampleResponse();
            Mockito.when(analysisService.analyzeWallet(VALID_ADDR, 3)).thenReturn(resp);

            mvc.perform(post("/api/v1/wallet/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletAddress").value(VALID_ADDR))
                    .andExpect(jsonPath("$.totalTransactions").value(5))
                    .andExpect(jsonPath("$.topCounterparties[0].address")
                            .value("0xAAAAbbbbCCCCddddEEEEffff0000111122223333"))
                    .andExpect(jsonPath("$.topCounterparties[0].transactionCount").value(42))
                    .andExpect(jsonPath("$.topCounterparties[0].name").value("Example Protocol"))
                    .andExpect(jsonPath("$.topCounterparties[0].type").value("PROTOCOL"))
                    .andExpect(jsonPath("$.topCounterparties[0].protocol").value("ExampleProtocol"))
                    // <-- updated here:
                    .andExpect(jsonPath("$.topCounterparties[0].knownEntity").value(true))
                    .andExpect(jsonPath("$.topCounterparties[0].etherscanUrl")
                            .value("https://etherscan.io/address/0xAAAAbbbbCCCCddddEEEEffff0000111122223333"));
        }
    }


    @Nested
    @DisplayName("GET /api/v1/wallet/analyze/{address}")
    class GetAnalyze {

        @Test
        @DisplayName("200 and correct JSON on valid address with default limit")
        void validGet() throws Exception {
            WalletAnalysisResponse resp = sampleResponse();
            Mockito.when(analysisService.analyzeWallet(VALID_ADDR, 10))
                    .thenReturn(resp);

            mvc.perform(get("/api/v1/wallet/analyze/{address}", VALID_ADDR)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletAddress").value(VALID_ADDR))
                    .andExpect(jsonPath("$.totalTransactions").value(5))
                    .andExpect(jsonPath("$.topCounterparties[0].address")
                            .value("0xAAAAbbbbCCCCddddEEEEffff0000111122223333"));
        }
    }
}

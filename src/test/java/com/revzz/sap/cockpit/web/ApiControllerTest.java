package com.revzz.sap.cockpit.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the health endpoint reports the monitored volumes")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.interfaces").value(greaterThan(0)))
                .andExpect(jsonPath("$.messagesInWindow").value(greaterThan(0)));
    }

    @Test
    @DisplayName("the summary endpoint aggregates the window")
    void summaryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interfaces").isNumber())
                .andExpect(jsonPath("$.messages").isNumber())
                .andExpect(jsonPath("$.errors").isNumber())
                .andExpect(jsonPath("$.openAlerts").isNumber());
    }

    @Test
    @DisplayName("the interfaces endpoint returns the SLA report")
    void interfacesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].successRate").exists());
    }

    @Test
    @DisplayName("the clusters endpoint returns the triage")
    void clustersEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].errorCode").exists())
                .andExpect(jsonPath("$[0].occurrences").exists());
    }

    @Test
    @DisplayName("the error catalogue is exposed")
    void errorsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].remediation").exists());
    }

    @Test
    @DisplayName("the alerts endpoint returns the inbox")
    void alertsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("an interface detail is reachable by name")
    void interfaceDetailEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/interfaces/ZBANK_FILE_IDOC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interfaceName").value("ZBANK_FILE_IDOC"))
                .andExpect(jsonPath("$.errors").value(greaterThan(0)));
    }
}

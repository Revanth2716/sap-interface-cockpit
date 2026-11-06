package com.revzz.sap.cockpit.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders every screen.
 *
 * <p>A template can compile and the service layer can be fully tested while
 * one screen still returns HTTP 500, because a single expression in the
 * markup does not resolve. This test renders each page the way a browser
 * would, so that class of bug fails the build.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} renders")
    @ValueSource(strings = {
            "/",
            "/interfaces",
            "/interfaces/ZBANK_FILE_IDOC",
            "/interfaces/ZSALES_ORDER_BAPI",
            "/errors",
            "/errors/ERR_AUTH_403",
            "/alerts",
            "/replay",
    })
    @DisplayName("every screen renders for the operations lead")
    void everyScreenRenders(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("OPS_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))))
                .andExpect(content().string(not(containsString("TemplateProcessingException"))));
    }

    @ParameterizedTest(name = "{0} renders for the analyst")
    @ValueSource(strings = {"/", "/interfaces", "/errors", "/alerts"})
    @DisplayName("the analyst role sees the monitoring screens")
    void analystScreensRender(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @ParameterizedTest(name = "the viewer cannot open {0}")
    @ValueSource(strings = {"/replay", "/alerts"})
    @DisplayName("the viewer role is blocked from the operations screens")
    void viewerIsBlocked(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("the dashboard shows the trend chart")
    void dashboardShowsTheTrend() throws Exception {
        mockMvc.perform(get("/").session(sessionFor("OPS_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("an unknown interface shows the error page, not a stack trace")
    void unknownInterfaceShowsErrorPage() throws Exception {
        mockMvc.perform(get("/interfaces/Z_DOES_NOT_EXIST").session(sessionFor("OPS_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Something went wrong")));
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.SESSION_ATTRIBUTE, role);
        session.setAttribute("DISPLAY_NAME", role);
        return session;
    }
}

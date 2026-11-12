package com.revzz.sap.upgrade.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The REST API.
 *
 * <p>Read-only, and authenticated - the data describes how close a
 * production system is to being un-upgradeable, so it is not served to
 * anyone who asks.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the health check is open")
    void healthIsOpen() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")))
                .andExpect(jsonPath("$.systems").value(3))
                .andExpect(jsonPath("$.blockers").value(3))
                .andExpect(jsonPath("$.averageScore").value(50));
    }

    @Test
    @DisplayName("the readiness data needs a session")
    void readinessNeedsASession() throws Exception {
        mockMvc.perform(get("/api/v1/readiness"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("authentication required")));
    }

    @Test
    @DisplayName("the readiness data is served to a signed-in caller")
    void readinessIsServedToASignedInCaller() throws Exception {
        mockMvc.perform(get("/api/v1/readiness").session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(content().string(containsString("PRD")))
                .andExpect(content().string(containsString("OBSOLETE_COMPONENT")));
    }

    @Test
    @DisplayName("one system can be read on its own")
    void oneSystemCanBeRead() throws Exception {
        mockMvc.perform(get("/api/v1/systems/DEV").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"score\":100")))
                .andExpect(content().string(containsString("\"severity\":\"INFO\"")));
    }

    @Test
    @DisplayName("an unknown system is a 404 with a JSON body")
    void unknownSystemIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/systems/NOPE").session(sessionFor("ANALYST")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("no such system")));
    }

    @Test
    @DisplayName("the findings list is served")
    void findingsAreServed() throws Exception {
        mockMvc.perform(get("/api/v1/findings").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(content().string(containsString("recommendation")));
    }

    @Test
    @DisplayName("the changelog is served")
    void changelogIsServed() throws Exception {
        mockMvc.perform(get("/api/v1/changelog").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(content().string(containsString("INCOMPATIBLE")));
    }

    @Test
    @DisplayName("one support package can be read on its own")
    void onePackageCanBeRead() throws Exception {
        mockMvc.perform(get("/api/v1/changelog/SP07").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"breakingCount\":1")))
                .andExpect(content().string(containsString("SAP_ABA")));
    }

    @Test
    @DisplayName("an unknown support package is a 404")
    void unknownPackageIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/changelog/SP99").session(sessionFor("ANALYST")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("no such support package")));
    }

    @Test
    @DisplayName("signing in with the demo password works, and a wrong password does not")
    void signInWorks() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "upgrade.manager")
                        .param("password", "demo1234"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/login")
                        .param("username", "upgrade.manager")
                        .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/login?error=invalid"));
    }

    @Test
    @DisplayName("the API never returns a stack trace")
    void apiNeverLeaksAStackTrace() throws Exception {
        mockMvc.perform(get("/api/v1/systems/NOPE").session(sessionFor("ANALYST")))
                .andExpect(content().string(not(containsString("at com.revzz"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.SESSION_ATTRIBUTE, role);
        session.setAttribute("DISPLAY_NAME", role);
        return session;
    }
}

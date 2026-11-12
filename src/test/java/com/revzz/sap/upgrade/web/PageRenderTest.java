package com.revzz.sap.upgrade.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
            "/systems/DEV",
            "/systems/QA",
            "/systems/PRD",
            "/changelog",
            "/changelog/SP07",
            "/report",
    })
    @DisplayName("every screen renders for the upgrade manager")
    void everyScreenRenders(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))))
                .andExpect(content().string(not(containsString("TemplateProcessingException"))));
    }

    @Test
    @DisplayName("the login screen renders")
    void loginRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in")));
    }

    @Test
    @DisplayName("the overview shows both charts and the blockers")
    void overviewShowsTheCharts() throws Exception {
        mockMvc.perform(get("/").session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("COMPONENT STATE MATRIX")))
                .andExpect(content().string(containsString("SAP_MDM")))
                .andExpect(content().string(containsString("OBSOLETE_COMPONENT")));
    }

    @Test
    @DisplayName("the system screen shows the component states and the findings")
    void systemScreenShowsTheComponents() throws Exception {
        mockMvc.perform(get("/systems/PRD").session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SAP_ST-PI")))
                .andExpect(content().string(containsString("COMPONENT_DEPENDENCY_BROKEN")))
                .andExpect(content().string(containsString("blocks")));
    }

    @Test
    @DisplayName("the changelog screen lists the incompatible changes")
    void changelogScreenShowsTheBreakingChanges() throws Exception {
        mockMvc.perform(get("/changelog").session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INCOMPATIBLE")))
                .andExpect(content().string(containsString("SP07")));
    }

    @Test
    @DisplayName("an unknown system shows the error page, not a stack trace")
    void unknownSystemShowsErrorPage() throws Exception {
        mockMvc.perform(get("/systems/NOPE").session(sessionFor("UPGRADE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Something went wrong")));
    }

    @ParameterizedTest(name = "the viewer cannot open {0}")
    @ValueSource(strings = {"/changelog", "/report"})
    @DisplayName("the viewer role is blocked from the analysis screens")
    void viewerIsBlocked(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an anonymous visitor is sent to the login page")
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String location = result.getResponse().getRedirectedUrl();
                    org.junit.jupiter.api.Assertions.assertNotNull(location);
                    org.junit.jupiter.api.Assertions.assertTrue(location.contains("/login"),
                            "expected a redirect to the login page but got " + location);
                });
    }

    @Test
    @DisplayName("the release analyst may read the changelog but may not record a patch failure")
    void analystCannotRecordAPatchFailure() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/systems/PRD/patch-error")
                .param("componentId", "SAP_KERNEL")
                .param("spId", "SP05")
                .session(sessionFor("ANALYST")))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.SESSION_ATTRIBUTE, role);
        session.setAttribute("DISPLAY_NAME", role);
        return session;
    }
}

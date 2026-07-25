package az.legalai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import az.legalai.document.controller.AdminDashboardController;
import az.legalai.document.service.DocumentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminDashboardController.class, LoginController.class})
@Import(SecurityConfig.class)
@TestPropertySource(
        properties = {"app.security.username=demo-admin", "app.security.password=demo-password"})
class SecurityConfigTest {
    @Autowired MockMvc mvc;

    @Value("${server.forward-headers-strategy}")
    String forwardedHeadersStrategy;

    @MockBean DocumentQueryService queries;

    @Test
    void redirectsAnonymousHtmlRequestsToLoginPage() throws Exception {
        mvc.perform(get("/admin").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void rendersDedicatedLoginPage() throws Exception {
        mvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void authenticatesSuccessfulProxiedLogin() throws Exception {
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .header("X-Forwarded-Proto", "https")
                                .header("X-Forwarded-Host", "legalai.jo3.org")
                                .param("username", "demo-admin")
                                .param("password", "demo-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void honorsReverseProxyForwardedHeaders() {
        assertThat(forwardedHeadersStrategy).isEqualTo("framework");
    }
}

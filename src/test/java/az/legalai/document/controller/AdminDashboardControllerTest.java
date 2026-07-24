package az.legalai.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.legalai.document.service.DocumentQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

class AdminDashboardControllerTest {
    @Test
    void rendersDashboardSummary() {
        DocumentQueryService queries = mock(DocumentQueryService.class);
        when(queries.list()).thenReturn(List.of());
        AdminDashboardController controller = new AdminDashboardController(queries);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("totalDocuments")).isEqualTo(0L);
        assertThat(model.getAttribute("completedDocuments")).isEqualTo(0L);
        assertThat(model.getAttribute("failedDocuments")).isEqualTo(0L);
        assertThat(model.getAttribute("totalChunks")).isEqualTo(0L);
    }
}

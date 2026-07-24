package az.legalai.document.controller;

import az.legalai.document.domain.DocumentStatus;
import az.legalai.document.service.DocumentQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final DocumentQueryService queries;

    public AdminDashboardController(DocumentQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        var documents = queries.list();
        Map<UUID, Long> chunkCounts = new LinkedHashMap<>();
        documents.forEach(
                document ->
                        chunkCounts.put(document.getId(), queries.chunkCount(document.getId())));

        long completed =
                documents.stream()
                        .filter(document -> document.getStatus() == DocumentStatus.COMPLETED)
                        .count();
        long failed =
                documents.stream()
                        .filter(document -> document.getStatus() == DocumentStatus.FAILED)
                        .count();
        long processing = documents.size() - completed - failed;
        long chunks = chunkCounts.values().stream().mapToLong(Long::longValue).sum();

        model.addAttribute("totalDocuments", (long) documents.size());
        model.addAttribute("completedDocuments", completed);
        model.addAttribute("processingDocuments", processing);
        model.addAttribute("failedDocuments", failed);
        model.addAttribute("totalChunks", chunks);
        model.addAttribute("recentDocuments", documents.stream().limit(5).toList());
        model.addAttribute("chunkCounts", chunkCounts);
        return "dashboard";
    }
}

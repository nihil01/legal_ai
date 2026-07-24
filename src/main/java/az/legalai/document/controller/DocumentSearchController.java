package az.legalai.document.controller;

import az.legalai.document.service.DocumentQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DocumentSearchController {
    private final DocumentQueryService service;

    public DocumentSearchController(DocumentQueryService s) {
        service = s;
    }

    @GetMapping("/admin/search")
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int limit,
            Model model) {
        model.addAttribute("query", query);
        model.addAttribute("limit", Math.max(1, Math.min(limit, 50)));
        model.addAttribute("results", service.search(query, limit));
        return "search";
    }
}

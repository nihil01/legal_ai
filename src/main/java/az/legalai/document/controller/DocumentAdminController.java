package az.legalai.document.controller;

import az.legalai.document.domain.DocumentType;
import az.legalai.document.service.*;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/documents")
public class DocumentAdminController {
    private static final Logger log = LoggerFactory.getLogger(DocumentAdminController.class);

    private final DocumentUploadService uploads;
    private final DocumentQueryService queries;
    private final DocumentLifecycleService lifecycle;

    public DocumentAdminController(
            DocumentUploadService u, DocumentQueryService q, DocumentLifecycleService l) {
        uploads = u;
        queries = q;
        lifecycle = l;
    }

    @GetMapping
    public String list(Model model) {
        var docs = queries.list();
        Map<UUID, Long> counts = new HashMap<>();
        docs.forEach(d -> counts.put(d.getId(), queries.chunkCount(d.getId())));
        model.addAttribute("documents", docs);
        model.addAttribute("chunkCounts", counts);
        return "documents/list";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("documentTypes", DocumentType.values());
        return "documents/upload";
    }

    @PostMapping("/upload")
    public String upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate adoptionDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate effectiveDate,
            @RequestParam(required = false) String language,
            RedirectAttributes flash) {
        try {
            UUID id =
                    uploads.upload(
                            file,
                            new UploadCommand(
                                    title,
                                    documentType,
                                    sourceUrl,
                                    adoptionDate,
                                    effectiveDate,
                                    language));
            flash.addFlashAttribute("message", "Документ загружен и поставлен в очередь");
            return "redirect:/admin/documents/" + id;
        } catch (DocumentValidationException | DuplicateDocumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/documents/upload";
        } catch (RuntimeException e) {
            log.error("Unexpected document upload failure", e);
            flash.addFlashAttribute(
                    "error", "Не удалось загрузить документ. Проверьте файл и повторите попытку");
            return "redirect:/admin/documents/upload";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable UUID id, Model model) {
        model.addAttribute("document", queries.get(id));
        model.addAttribute("chunks", queries.chunks(id));
        model.addAttribute("chunkCount", queries.chunkCount(id));
        return "documents/details";
    }

    @PostMapping("/{id}/reprocess")
    public String reprocess(@PathVariable UUID id, RedirectAttributes f) {
        try {
            lifecycle.reprocess(id);
            f.addFlashAttribute("message", "Повторная обработка поставлена в очередь");
        } catch (RuntimeException e) {
            log.error("Failed to reprocess document {}", id, e);
            f.addFlashAttribute("error", "Не удалось запустить повторную обработку");
        }
        return "redirect:/admin/documents/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes f) {
        try {
            lifecycle.delete(id);
            f.addFlashAttribute("message", "Документ удалён");
            return "redirect:/admin/documents";
        } catch (RuntimeException e) {
            log.error("Failed to delete document {}", id, e);
            f.addFlashAttribute("error", "Не удалось удалить документ");
            return "redirect:/admin/documents/" + id;
        }
    }
}

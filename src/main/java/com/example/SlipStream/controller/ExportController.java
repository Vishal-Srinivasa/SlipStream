package com.example.SlipStream.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.service.PageService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Controller
public class ExportController {

    @Autowired
    private PageService pageService;

    @GetMapping("/export/{pageId}")
    public ResponseEntity<InputStreamResource> exportPageAsPdf(@PathVariable String pageId)
            throws IOException, InterruptedException, ExecutionException {

        // Fetch the root page
        PageComponent rootPage = pageService.getPage(pageId);
        if (rootPage == null) {
            throw new IllegalArgumentException("Page not found");
        }

        // Recursively load children
        expandSubpages(rootPage);

        // Generate the full HTML
        String htmlContent = generateHtmlContent(rootPage);

        // Convert HTML to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(htmlContent, null);
        builder.toStream(outputStream);
        builder.run();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"page.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(inputStream));
    }

    // Expand all subpages recursively
    private void expandSubpages(PageComponent page) throws ExecutionException, InterruptedException {
        if (page.getChildren() != null) {
            for (int i = 0; i < page.getChildren().size(); i++) {
                PageComponent childStub = page.getChildren().get(i);
                PageComponent fullChild = pageService.getPage(childStub.getPageId());
                page.getChildren().set(i, fullChild);
                expandSubpages(fullChild);
            }
        }
    }

    // Build full HTML with recursive rendering
    private String generateHtmlContent(PageComponent page) {
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html>");
        htmlBuilder.append("<head>");
        htmlBuilder.append("<title>").append(page.getTitle()).append("</title>");
        htmlBuilder.append("<style>");
        htmlBuilder.append("body { font-family: Arial, sans-serif; line-height: 1.6; padding: 20px; }");
        htmlBuilder.append("h1, h2, h3, h4, h5, h6 { color: #333; margin-top: 24px; }");
        htmlBuilder.append("p { margin-bottom: 10px; }");
        htmlBuilder.append(".block { margin-bottom: 30px; }");
        htmlBuilder.append("</style>");
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");

        renderPageRecursive(htmlBuilder, page, 1);  // Start with heading level 1

        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");

        return htmlBuilder.toString();
    }

    // Recursively render content and children
    private void renderPageRecursive(StringBuilder builder, PageComponent page, int level) {
        builder.append("<div class='block'>");
        builder.append("<h").append(level).append(">").append(page.getTitle()).append("</h").append(level).append(">");

        if (page.getContent() != null && !page.getContent().isEmpty()) {
            String[] lines = page.getContent().split("\\r?\\n");
            for (String line : lines) {
                builder.append("<p>").append(line).append("</p>");
            }
        }

        builder.append("</div>");

        if (page.getChildren() != null && !page.getChildren().isEmpty()) {
            for (PageComponent child : page.getChildren()) {
                renderPageRecursive(builder, child, Math.min(level + 1, 6));  // Use h1 to h6
            }
        }
    }
}

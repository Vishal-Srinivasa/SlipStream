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

        // Fetch the root page using the service method
        PageComponent rootPage = pageService.getPage(pageId);
        if (rootPage == null) {
            throw new IllegalArgumentException("Page not found");
        }

        // Recursively load children
        expandSubpages(rootPage);

        // Generate HTML content manually
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

    // Recursively load children using the existing getChildren() and getPageId() methods
    private void expandSubpages(PageComponent page) throws ExecutionException, InterruptedException {
        if (page.getChildren() != null) {
            for (int i = 0; i < page.getChildren().size(); i++) {
                PageComponent childStub = page.getChildren().get(i);
                PageComponent fullChild = pageService.getPage(childStub.getPageId());
                page.getChildren().set(i, fullChild);
                expandSubpages(fullChild); // Recursively expand children
            }
        }
    }

    // Generate HTML content manually
    private String generateHtmlContent(PageComponent page) {
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html>");
        htmlBuilder.append("<head>");
        htmlBuilder.append("<title>").append(page.getTitle()).append("</title>");
        htmlBuilder.append("<style>");
        htmlBuilder.append("body { font-family: Arial, sans-serif; }");
        htmlBuilder.append("h1 { color: #333; }");
        htmlBuilder.append("</style>");
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");
        htmlBuilder.append("<h1>").append(page.getTitle()).append("</h1>");
        htmlBuilder.append("<p>").append(page.getContent()).append("</p>");

        if (page.getChildren() != null && !page.getChildren().isEmpty()) {
            htmlBuilder.append("<h2>Subpages</h2>");
            htmlBuilder.append("<ul>");
            for (PageComponent child : page.getChildren()) {
                htmlBuilder.append("<li>").append(child.getTitle()).append("</li>");
            }
            htmlBuilder.append("</ul>");
        }

        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");

        return htmlBuilder.toString();
    }
}

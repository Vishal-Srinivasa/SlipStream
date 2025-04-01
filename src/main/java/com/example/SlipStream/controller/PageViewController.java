package com.example.SlipStream.controller;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.SlipStream.model.Page;
import com.example.SlipStream.service.PageService;

@Controller
@RequestMapping("/view/pages")
public class PageViewController {

    private final PageService pageService;

    @Autowired
    public PageViewController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{pageId}")
    public String viewPage(@PathVariable String pageId, Model model) {
        try {
            Page page = pageService.getPage(pageId);
            if (page != null) {
                model.addAttribute("title", page.getTitle());
                model.addAttribute("content", page.getContent());
                model.addAttribute("pageId", page.getPageId());
                model.addAttribute("createdAt", page.getCreatedAt());
                model.addAttribute("lastUpdated", page.getLastUpdated());
                model.addAttribute("owner", page.getOwner());
                model.addAttribute("children", page.getChildren());
                return "page_template"; // This will look for page_template.html
            } else {
                return "error";
            }
        } catch (InterruptedException | ExecutionException e) {
            model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreatePage() {
        return "create_page"; // This will look for create_page.html
    }

    @GetMapping("/edit/{pageId}")
    public String showEditPage(@PathVariable String pageId, Model model) {
        try {
            Page page = pageService.getPage(pageId);
            if (page != null) {
                model.addAttribute("page", page);
                return "edit_page"; // This will look for edit_page.html
            } else {
                return "error";
            }
        } catch (InterruptedException | ExecutionException e) {
            model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
            return "error";
        }
    }
}

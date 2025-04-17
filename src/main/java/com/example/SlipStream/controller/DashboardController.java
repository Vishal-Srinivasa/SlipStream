package com.example.SlipStream.controller;

import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final PageService pageService;

    @Autowired
    public DashboardController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public String showDashboard(Model model) {
        String currentUserEmail = getCurrentUserEmail();
        if (currentUserEmail == null) {
            logger.warn("Cannot show dashboard, user not authenticated.");
            return "redirect:/login"; // Redirect to login if user somehow isn't authenticated
        }

        logger.info("Loading dashboard for user: {}", currentUserEmail);
        List<PageComponent> ownedPages = new ArrayList<>();
        List<PageComponent> sharedPages = new ArrayList<>();

        try {
            List<PageComponent> allPages = pageService.getAllPages(); // Fetch all pages

            for (PageComponent page : allPages) {
                if (page.getOwner() != null && page.getOwner().equals(currentUserEmail)) {
                    ownedPages.add(page);
                } else {
                    // Check if the page is shared with the current user
                    Map<String, String> sharingInfo = page.getSharingInfo();
                    if (sharingInfo != null && sharingInfo.containsKey(currentUserEmail)) {
                        sharedPages.add(page);
                    }
                    // We could also add publicly published pages here if desired,
                    // but the prompt specifically asked for owned and shared.
                }
            }

            logger.debug("User {} owns {} pages and has {} pages shared with them.", currentUserEmail, ownedPages.size(), sharedPages.size());

        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error fetching pages for dashboard for user {}: {}", currentUserEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Could not load page data. Please try again later.");
            // Keep ownedPages and sharedPages as empty lists
        }

        model.addAttribute("ownedPages", ownedPages);
        model.addAttribute("sharedPages", sharedPages);
        model.addAttribute("currentUserEmail", currentUserEmail); // Add user email for display

        return "dashboard"; // Name of the Thymeleaf template
    }

    // Helper method to get current user email
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return authentication.getName();
            }
        }
        return null;
    }
}

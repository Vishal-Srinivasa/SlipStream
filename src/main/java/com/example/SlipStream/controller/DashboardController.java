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
            // SecurityConfig should redirect to login, but double-check
            return "redirect:/login?error=session_expired";
        }

        logger.info("Loading dashboard for user: {}", currentUserEmail);
        List<PageComponent> ownedPages = new ArrayList<>();
        List<PageComponent> sharedPages = new ArrayList<>();

        try {
            // Fetch all pages - NOTE: This can be inefficient for large numbers of pages.
            // Consider adding specific repository/service methods like getPagesOwnedBy(email)
            // and getPagesSharedWith(email) if performance becomes an issue.
            List<PageComponent> allAccessiblePages = pageService.getAllPages(); // Assuming this gets all pages user might see

            for (PageComponent page : allAccessiblePages) {
                boolean isOwner = page.getOwner() != null && page.getOwner().equals(currentUserEmail);
                boolean isSharedWithUser = page.getSharingInfo() != null && page.getSharingInfo().containsKey(currentUserEmail);

                if (isOwner) {
                    // Add to owned list (even if also shared, owner takes precedence)
                    ownedPages.add(page);
                } else if (isSharedWithUser) {
                    // Add to shared list only if not the owner
                    sharedPages.add(page);
                }
                // We could also add publicly published pages user doesn't own/isn't shared with,
                // but the requirement focused on owned and shared.
            }

            logger.debug("User {} owns {} pages and has {} pages shared with them.", currentUserEmail, ownedPages.size(), sharedPages.size());

        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error fetching pages for dashboard for user {}: {}", currentUserEmail, e.getMessage(), e);
            Thread.currentThread().interrupt(); // Re-interrupt thread
            model.addAttribute("errorMessage", "Could not load page data. Please try again later.");
            // Keep ownedPages and sharedPages as empty lists
        } catch (Exception e) {
             logger.error("Unexpected error fetching pages for dashboard for user {}: {}", currentUserEmail, e.getMessage(), e);
             model.addAttribute("errorMessage", "An unexpected error occurred while loading page data.");
        }


        // Sort pages alphabetically by title for consistent display
        ownedPages.sort((p1, p2) -> String.CASE_INSENSITIVE_ORDER.compare(p1.getTitle() != null ? p1.getTitle() : "", p2.getTitle() != null ? p2.getTitle() : ""));
        sharedPages.sort((p1, p2) -> String.CASE_INSENSITIVE_ORDER.compare(p1.getTitle() != null ? p1.getTitle() : "", p2.getTitle() != null ? p2.getTitle() : ""));


        model.addAttribute("ownedPages", ownedPages);
        model.addAttribute("sharedPages", sharedPages);
        model.addAttribute("currentUserEmail", currentUserEmail); // Add user email for display

        return "dashboard"; // Name of the Thymeleaf template (dashboard.html)
    }

    // Helper method to get current user email
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Check principal is not null and not the anonymousUser string
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername(); // Typically the email
            } else {
                // Fallback if principal is just a String (e.g., from token directly)
                return authentication.getName();
            }
        }
        return null; // Return null if not authenticated or principal is anonymous/null
    }
}

package com.example.SlipStream.controller;

import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.model.Workspace; // Import Workspace
import com.example.SlipStream.service.PageService;
import com.example.SlipStream.service.WorkspaceService; // Import WorkspaceService
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
import java.util.Collections; // Import Collections
import java.util.Comparator; // Import Comparator
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final PageService pageService;
    private final WorkspaceService workspaceService; // Add WorkspaceService

    // Define PageNode inner class
    public static class PageNode {
        PageComponent page;
        List<PageNode> children = new ArrayList<>();

        public PageNode(PageComponent page) {
            this.page = page;
        }

        public PageComponent getPage() {
            return page;
        }

        public List<PageNode> getChildren() {
            return children;
        }

        public void addChild(PageNode child) {
            this.children.add(child);
        }

        // Recursive sort method
        public void sortChildrenRecursively(Comparator<PageNode> comparator) {
            this.children.sort(comparator);
            for (PageNode child : this.children) {
                child.sortChildrenRecursively(comparator);
            }
        }
    }

    @Autowired
    public DashboardController(PageService pageService, WorkspaceService workspaceService) { // Inject WorkspaceService
        this.pageService = pageService;
        this.workspaceService = workspaceService; // Initialize WorkspaceService
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                // Fallback for non-UserDetails principals
                return authentication.getName();
            }
        }
        return null;
    }

    @GetMapping
    public String showDashboard(Model model) {
        String currentUserEmail = getCurrentUserEmail();
        if (currentUserEmail == null) {
            logger.warn("Cannot show dashboard, user not authenticated.");
            return "redirect:/login?error=session_expired";
        }

        logger.info("Loading dashboard for user: {}", currentUserEmail);
        List<PageNode> rootPageNodes = new ArrayList<>(); // List for hierarchical page structure
        List<Workspace> workspaces = new ArrayList<>(); // List for workspaces

        try {
            // Fetch all pages accessible by the current user
            List<PageComponent> allAccessiblePages = pageService.getAllPages();
            logger.debug("Fetched {} accessible pages for user {}", allAccessiblePages.size(), currentUserEmail);

            // Build hierarchy
            Map<String, PageNode> nodeMap = new HashMap<>();
            Map<String, PageComponent> pageMap = allAccessiblePages.stream()
                    .collect(Collectors.toMap(PageComponent::getPageId, page -> page));

            // Create nodes and identify roots
            for (PageComponent page : allAccessiblePages) {
                PageNode node = new PageNode(page);
                nodeMap.put(page.getPageId(), node);

                String parentId = page.getParentPageId();
                // A page is a root if it has no parent OR its parent is not in the accessible list
                if (parentId == null || parentId.isEmpty() || !pageMap.containsKey(parentId)) {
                    rootPageNodes.add(node);
                    logger.trace("Identified root page: {} ({})", page.getTitle(), page.getPageId());
                }
            }

            // Link children to parents
            for (PageNode node : nodeMap.values()) {
                String parentId = node.getPage().getParentPageId();
                if (parentId != null && !parentId.isEmpty() && nodeMap.containsKey(parentId)) {
                    PageNode parentNode = nodeMap.get(parentId);
                    if (parentNode != null) {
                        parentNode.addChild(node);
                        logger.trace("Linked child {} to parent {}", node.getPage().getPageId(), parentId);
                    }
                }
            }

            // Sort root nodes and their children recursively
            Comparator<PageNode> pageNodeComparator = Comparator.comparing(node -> node.getPage().getTitle() != null ? node.getPage().getTitle() : "", String.CASE_INSENSITIVE_ORDER);
            rootPageNodes.sort(pageNodeComparator);
            for (PageNode rootNode : rootPageNodes) {
                rootNode.sortChildrenRecursively(pageNodeComparator);
            }
            logger.debug("Built hierarchy with {} root nodes.", rootPageNodes.size());

            // Fetch workspaces
            workspaces = workspaceService.getWorkspacesForUser(currentUserEmail);
            logger.debug("User {} is a member of {} workspaces.", currentUserEmail, workspaces.size());

        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error fetching data for dashboard for user {}: {}", currentUserEmail, e.getMessage(), e);
            Thread.currentThread().interrupt();
            model.addAttribute("errorMessage", "Could not load dashboard data. Please try again later.");
            // Keep lists empty
        } catch (Exception e) {
            logger.error("Unexpected error fetching data for dashboard for user {}: {}", currentUserEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred while loading dashboard data.");
        }

        // Sort workspaces alphabetically by name
        workspaces.sort(Comparator.comparing(Workspace::getName, String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("pageNodes", rootPageNodes); // Pass the hierarchical structure
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("currentUserEmail", currentUserEmail);

        return "dashboard";
    }
}

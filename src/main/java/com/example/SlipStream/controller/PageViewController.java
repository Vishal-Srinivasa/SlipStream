package com.example.SlipStream.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.example.SlipStream.model.ContainerPage;
import com.example.SlipStream.model.ContentPage;
import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.service.PageService;

@Controller
@RequestMapping("/view/pages")
public class PageViewController {

    private final PageService pageService;
    private static final Logger logger = LoggerFactory.getLogger(PageViewController.class);

    @Autowired
    public PageViewController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{pageId}")
    public String viewPage(@PathVariable String pageId, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        boolean canEdit = false;
        PageComponent page = null;

        try {
            page = pageService.getPage(pageId);

            if (page == null) {
                logger.warn("PageService returned null for pageId: {}. Assuming not found.", pageId);
                model.addAttribute("errorMessage", "Page not found.");
                return "error";
            }

            try {
                pageService.getPageForEditing(pageId);
                canEdit = true;
                logger.debug("User has edit access to page {}", pageId);
            } catch (AccessDeniedException editDeniedException) {
                canEdit = false;
                logger.debug("User has view-only access to page {}: {}", pageId, editDeniedException.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error checking edit access for page {}: {}", pageId, e.getMessage());
                canEdit = false;
            }

            model.addAttribute("title", page.getTitle());
            model.addAttribute("content", page.getContent());
            model.addAttribute("pageId", page.getPageId());
            model.addAttribute("createdAt", page.getCreatedAt());
            model.addAttribute("lastUpdated", page.getLastUpdated());
            model.addAttribute("owner", page.getOwner());
            model.addAttribute("isContainer", !page.isLeaf());
            model.addAttribute("isPublished", page.isPublished());
            model.addAttribute("sharingInfo", page.getSharingInfo());
            model.addAttribute("canEdit", canEdit);

            List<PageComponent> childPages = pageService.getChildPages(pageId);
            model.addAttribute("childPages", childPages);

            logger.info("Viewing Page: {} (ID: {}), Can Edit: {}", page.getTitle(), pageId, canEdit);

            return "page_template";

        } catch (AccessDeniedException viewDeniedException) {
            String currentUserEmail = getCurrentUserEmail();
            logger.warn("Access Denied: User '{}' attempted to view page '{}'. Redirecting to login.",
                        currentUserEmail != null ? currentUserEmail : "anonymous", pageId);

            String continueUrl = request.getRequestURI();
            if (request.getQueryString() != null) {
                continueUrl += "?" + request.getQueryString();
            }
            redirectAttributes.addFlashAttribute("login_error", "You need to log in to view that page.");
            return "redirect:/login?continue=" + continueUrl;

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error retrieving page {}: {}", pageId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving page", e);
        }
    }

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

    @GetMapping("/create")
    public String showCreatePage(@RequestParam(required = false) String parentId, Model model) {
        model.addAttribute("parentId", parentId);
        model.addAttribute("pageType", "content");
        return "create_page";
    }
    
    @GetMapping("/create/container")
    public String showCreateContainerPage(@RequestParam(required = false) String parentId, Model model) {
        model.addAttribute("parentId", parentId);
        model.addAttribute("pageType", "container");
        return "create_page";
    }

    @PostMapping("/create")
    public String createPage(@RequestParam String title, 
                            @RequestParam(required = false) String parentId, 
                            @RequestParam(required = false) String owner,
                            @RequestParam String pageType) {
        try {
            String pageId;
            String pageOwner = owner;

            if (pageOwner == null || pageOwner.isEmpty()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
                    pageOwner = ((UserDetails) authentication.getPrincipal()).getUsername();
                } else if (authentication != null && authentication.isAuthenticated()) {
                    pageOwner = authentication.getName();
                }
            }

            if (pageOwner == null || pageOwner.isEmpty()) {
                return "error";
            }

            if ("container".equals(pageType)) {
                pageId = pageService.createContainerPage(title, "", parentId, pageOwner);
            } else {
                pageId = pageService.createContentPage(title, "", parentId, pageOwner);
            }
            
            return "redirect:/view/pages/" + pageId;
        } catch (IllegalStateException e) {
            return "error";
        } catch (InterruptedException | ExecutionException e) {
            return "error";
        }
    }

    @GetMapping("/edit/{pageId}")
    public String showEditPage(@PathVariable String pageId, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            PageComponent page = pageService.getPageForEditing(pageId);
            if (page != null) {
                model.addAttribute("page", page);
                model.addAttribute("isContainer", !page.isLeaf());
                return "edit_page";
            } else {
                model.addAttribute("errorMessage", "Page not found or access denied for editing.");
                return "error";
            }
        } catch (AccessDeniedException e) {
            logger.warn("Access Denied: User '{}' attempted to edit page '{}'. Redirecting to login.",
                        getCurrentUserEmail() != null ? getCurrentUserEmail() : "anonymous", pageId);
            String continueUrl = request.getRequestURI();
            redirectAttributes.addFlashAttribute("login_error", "You need to log in with edit permissions for that page.");
            return "redirect:/login?continue=" + continueUrl;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error retrieving page {} for editing: {}", pageId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Error retrieving page for editing: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving page for editing", e);
        }
    }
    
    @PostMapping("/edit/{pageId}")
    public String updatePage(@PathVariable String pageId, 
                            @RequestParam String title,
                            @RequestParam String content) {
        try {
            PageComponent page = pageService.getPageForEditing(pageId);
            if (page != null) {
                page.setTitle(title);
                
                if (page.isLeaf()) {
                    ((ContentPage) page).setContent(content);
                } else {
                    ((ContainerPage) page).setSummary(content);
                }
                
                pageService.createPage(page);
                return "redirect:/view/pages/" + pageId;
            } else {
                return "error";
            }
        } catch (AccessDeniedException e) {
            return "redirect:/view/pages/" + pageId + "?error=access_denied";
        } catch (InterruptedException | ExecutionException e) {
            return "redirect:/view/pages/" + pageId + "?error=update_failed";
        }
    }
}
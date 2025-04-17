package com.example.SlipStream.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.Collections; // Import Collections

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.service.PageService;

@RestController
@RequestMapping("/api/pages")
public class PageController {
    
    private final PageService pageService;
    
    @Autowired
    public PageController(PageService pageService) {
        this.pageService = pageService;
    }
    
    @PostMapping
    public ResponseEntity<String> createPage(@RequestBody PageRequestDTO pageDTO) {
        try {
            String pageId;
            String owner = pageDTO.getOwner();

            // If owner is not provided in DTO, get it from authenticated principal
            if (owner == null || owner.isEmpty()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
                    owner = ((UserDetails) authentication.getPrincipal()).getUsername(); // Assuming username is email
                } else if (authentication != null && authentication.isAuthenticated()) {
                     // Fallback if principal is not UserDetails (e.g., just a String)
                     owner = authentication.getName();
                }
            }

            if (owner == null || owner.isEmpty()) {
                 return new ResponseEntity<>("Cannot create page: Owner information is missing.", HttpStatus.BAD_REQUEST);
            }

            if (pageDTO.getType() != null && pageDTO.getType().equals("container")) {
                pageId = pageService.createContainerPage(
                    pageDTO.getTitle(), 
                    pageDTO.getContent(), 
                    pageDTO.getParentPageId(), 
                    owner
                );
            } else {
                pageId = pageService.createContentPage(
                    pageDTO.getTitle(), 
                    pageDTO.getContent(), 
                    pageDTO.getParentPageId(), 
                    owner
                );
            }
            
            return new ResponseEntity<>(pageId, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
             return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error creating page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{pageId}")
    public ResponseEntity<?> getPage(@PathVariable String pageId) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page != null) {
                return new ResponseEntity<>(page, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Page not found or access denied.", HttpStatus.NOT_FOUND);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error retrieving page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<PageComponent>> getAllPages() {
        try {
            List<PageComponent> pages = pageService.getAllPages();
            return new ResponseEntity<>(pages, HttpStatus.OK);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/children/{parentPageId}")
    public ResponseEntity<List<PageComponent>> getChildPages(@PathVariable String parentPageId) {
        try {
            List<PageComponent> childPages = pageService.getChildPages(parentPageId);
            return new ResponseEntity<>(childPages, HttpStatus.OK);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{pageId}")
    public ResponseEntity<String> updatePage(@PathVariable String pageId, @RequestBody UpdatePageRequestDTO updateDTO) {
        try {
            // Call the new service method
            boolean updated = pageService.updatePage(pageId, updateDTO.getTitle(), updateDTO.getContent());
            if (updated) {
                return new ResponseEntity<>("Page updated successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Page not found or update failed.", HttpStatus.NOT_FOUND);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt(); // Re-interrupt thread
            return new ResponseEntity<>("Error updating page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
             // Catch unexpected errors
             // Consider logging the exception e
             return new ResponseEntity<>("An unexpected error occurred while updating the page.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{pageId}")
    public ResponseEntity<List<String>> deletePage(@PathVariable String pageId) { // Changed return type
        try {
            List<String> deletedIds = pageService.deletePage(pageId);
            if (!deletedIds.isEmpty()) {
                // Return the list of deleted IDs on success
                return new ResponseEntity<>(deletedIds, HttpStatus.OK);
            } else {
                // If the list is empty, it might mean the page wasn't found
                // or deletion failed silently in the service/repo (less ideal).
                // Check service logic; assuming empty list means not found or nothing to delete.
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND);
            }
        } catch (AccessDeniedException e) {
            // Return empty list and Forbidden status
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            // Return empty list and Internal Server Error status
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/{pageId}/share")
    public ResponseEntity<String> sharePage(@PathVariable String pageId, @RequestBody ShareRequestDTO shareRequest) {
        try {
            boolean shared = pageService.sharePage(pageId, shareRequest.getUserEmail(), shareRequest.getAccessLevel());
            if (shared) {
                return new ResponseEntity<>("Page shared successfully with " + shareRequest.getUserEmail(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Failed to share page.", HttpStatus.BAD_REQUEST);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error sharing page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{pageId}/share")
    public ResponseEntity<String> unsharePage(@PathVariable String pageId, @RequestParam String userEmail) {
        try {
            boolean unshared = pageService.unsharePage(pageId, userEmail);
            if (unshared) {
                return new ResponseEntity<>("Sharing removed for " + userEmail, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Failed to unshare page.", HttpStatus.BAD_REQUEST);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error unsharing page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{pageId}/share")
    public ResponseEntity<?> getSharingInfo(@PathVariable String pageId) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page == null) {
                return new ResponseEntity<>("Page not found or access denied.", HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(page.getSharingInfo(), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error retrieving sharing info: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{pageId}/publish")
    public ResponseEntity<String> publishPage(@PathVariable String pageId) {
        try {
            boolean published = pageService.publishPage(pageId);
            if (published) {
                return new ResponseEntity<>("Page published successfully.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Failed to publish page.", HttpStatus.BAD_REQUEST);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error publishing page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{pageId}/publish")
    public ResponseEntity<String> unpublishPage(@PathVariable String pageId) {
        try {
            boolean unpublished = pageService.unpublishPage(pageId);
            if (unpublished) {
                return new ResponseEntity<>("Page unpublished successfully.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Failed to unpublish page.", HttpStatus.BAD_REQUEST);
            }
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error unpublishing page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static class PageRequestDTO {
        private String title;
        private String content;
        private String parentPageId;
        private String owner;
        private String type;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getParentPageId() {
            return parentPageId;
        }
        
        public void setParentPageId(String parentPageId) {
            this.parentPageId = parentPageId;
        }
        
        public String getOwner() {
            return owner;
        }
        
        public void setOwner(String owner) {
            this.owner = owner;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ShareRequestDTO {
        private String userEmail;
        private String accessLevel;

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getAccessLevel() {
            return accessLevel;
        }

        public void setAccessLevel(String accessLevel) {
            this.accessLevel = accessLevel;
        }
    }

    public static class UpdatePageRequestDTO {
        private String title;
        private String content;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
package com.example.SlipStream.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            
            if (pageDTO.getType() != null && pageDTO.getType().equals("container")) {
                // Create a container page
                pageId = pageService.createContainerPage(
                    pageDTO.getTitle(), 
                    pageDTO.getContent(), 
                    pageDTO.getParentPageId(), 
                    pageDTO.getOwner()
                );
            } else {
                // Default to content page
                pageId = pageService.createContentPage(
                    pageDTO.getTitle(), 
                    pageDTO.getContent(), 
                    pageDTO.getParentPageId(), 
                    pageDTO.getOwner()
                );
            }
            
            // Return just the pageId as a string, which matches what the frontend expects
            return new ResponseEntity<>(pageId, HttpStatus.CREATED);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error creating page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{pageId}")
    public ResponseEntity<PageComponent> getPage(@PathVariable String pageId) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page != null) {
                return new ResponseEntity<>(page, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity<String> updatePageContent(@PathVariable String pageId, @RequestBody String newContent) {
        try {
            boolean updated = pageService.updatePageContent(pageId, newContent);
            if (updated) {
                return new ResponseEntity<>("Page updated successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Page not found", HttpStatus.NOT_FOUND);
            }
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error updating page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{pageId}")
    public ResponseEntity<String> deletePage(@PathVariable String pageId) {
        try {
            boolean deleted = pageService.deletePage(pageId);
            if (deleted) {
                return new ResponseEntity<>("Page deleted successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Page not found", HttpStatus.NOT_FOUND);
            }
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>("Error deleting page: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Simple DTO to handle page creation requests
    public static class PageRequestDTO {
        private String title;
        private String content;
        private String parentPageId;
        private String owner;
        private String type; // "content" or "container"
        
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
}
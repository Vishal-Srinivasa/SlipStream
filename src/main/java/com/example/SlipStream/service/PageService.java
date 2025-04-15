package com.example.SlipStream.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.SlipStream.model.ContainerPage;
import com.example.SlipStream.model.ContentPage;
import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.repository.PageRepository;

@Service
public class PageService {
    
    private final PageRepository pageRepository;
    
    @Autowired
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }
    
    /**
     * Creates a content page and updates parent-child relationship if needed
     */
    public String createContentPage(String title, String content, String parentPageId, String owner) 
            throws ExecutionException, InterruptedException {
        // Create the content page
        ContentPage page = new ContentPage(title, content, parentPageId, owner);
        String pageId = pageRepository.createPage(page);
        
        // If parent exists, update the parent-child relationship
        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }
        
        return pageId;
    }
    
    /**
     * Creates a container page and updates parent-child relationship if needed
     */
    public String createContainerPage(String title, String summary, String parentPageId, String owner) 
            throws ExecutionException, InterruptedException {
        // Create the container page
        ContainerPage page = new ContainerPage(title, summary, parentPageId, owner);
        String pageId = pageRepository.createPage(page);
        
        // If parent exists, update the parent-child relationship
        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }
        
        return pageId;
    }
    
    /**
     * Method to maintain compatibility with existing controller code
     */
    public String createPage(PageComponent page) throws ExecutionException, InterruptedException {
        String pageId = pageRepository.createPage(page);
        
        // If parent exists, update the parent-child relationship
        String parentPageId = page.getParentPageId();
        if (parentPageId != null && !parentPageId.isEmpty()) {
            updateParentChildRelationship(parentPageId, pageId);
        }
        
        return pageId;
    }


    // Modify the updateParentChildRelationship method in your PageService class
/**
     * Updates the parent-child relationship when a new page is created with a parent.
     * If the parent is a content page, it will be converted to a container page.
     * Only stores the ID of the child page in the parent's childrenIds list.
     */
    private void updateParentChildRelationship(String parentPageId, String childPageId) 
            throws ExecutionException, InterruptedException {
        
        PageComponent parentPage = pageRepository.getPage(parentPageId);
        
        if (parentPage == null) {
            return;
        }
        
        // If parent is a content page, convert it to a container page
        if (parentPage.isLeaf()) {
            ContentPage contentParent = (ContentPage) parentPage;
            
            // Create a new container page with the same properties
            ContainerPage newContainerPage = new ContainerPage(
                contentParent.getTitle(),
                contentParent.getContent(), // Use content as summary
                contentParent.getParentPageId(),
                contentParent.getOwner()
            );
            
            // Set the same ID
            newContainerPage.setPageId(parentPageId);
            
            // Add only the child ID to the container
            if (newContainerPage.getChildrenIds() == null) {
                newContainerPage.setChildrenIds(new ArrayList<>());
            }
            newContainerPage.getChildrenIds().add(childPageId);
            
            // Update the page in the database
            pageRepository.updatePage(newContainerPage);
        } else if (parentPage instanceof ContainerPage) {
            // Just add the child ID to the existing container
            ContainerPage containerParent = (ContainerPage) parentPage;
            
            // Make sure childrenIds is initialized
            if (containerParent.getChildrenIds() == null) {
                containerParent.setChildrenIds(new ArrayList<>());
            }
            
            // Add the child ID if it's not already in the list
            if (!containerParent.getChildrenIds().contains(childPageId)) {
                containerParent.getChildrenIds().add(childPageId);
                
                // Update the container page in the database
                pageRepository.updatePage(containerParent);
            }
        }
    }














    
    public PageComponent getPage(String pageId) throws ExecutionException, InterruptedException {
        return pageRepository.getPage(pageId);
    }
    
    public List<PageComponent> getAllPages() throws ExecutionException, InterruptedException {
        return pageRepository.getAllPages();
    }








    // Add or update this method to properly load children when needed
public List<PageComponent> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
    PageComponent parent = getPage(parentPageId);
    
    if (parent == null || parent.isLeaf()) {
        return new ArrayList<>();
    }
    
    ContainerPage containerParent = (ContainerPage) parent;
    List<String> childIds = containerParent.getChildrenIds();
    List<PageComponent> children = new ArrayList<>();
    
    for (String childId : childIds) {
        PageComponent child = getPage(childId);
        if (child != null) {
            children.add(child);
        }
    }
    
    // Set the loaded children on the parent for future in-memory operations
    containerParent.setLoadedChildren(children);
    
    return children;
}
    
    public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
        return pageRepository.updatePageContent(pageId, newContent);
    }
    
    /**
     * Deletes a page and all its child pages
     */
    public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
        // Before deleting, check if this page has children
        List<PageComponent> children = getChildPages(pageId);
        
        // If has children, delete all children first (recursive approach)
        for (PageComponent child : children) {
            deletePage(child.getPageId());
        }
        
        return pageRepository.deletePage(pageId);
    }
    
    /**
     * Checks if a page has children
     */
    public boolean hasChildren(String pageId) throws ExecutionException, InterruptedException {
        List<PageComponent> children = getChildPages(pageId);
        return !children.isEmpty();
    }
    
    /**
     * Converts a content page to a container page
     */
    public boolean convertToContainerPage(String pageId) throws ExecutionException, InterruptedException {
        PageComponent page = getPage(pageId);
        
        if (page == null || !page.isLeaf()) {
            return false; // Either page doesn't exist or is already a container
        }
        
        ContentPage contentPage = (ContentPage) page;
        
        // Create a container page with the same properties
        ContainerPage containerPage = new ContainerPage(
            contentPage.getTitle(),
            contentPage.getContent(), // Use content as summary
            contentPage.getParentPageId(),
            contentPage.getOwner()
        );
        containerPage.setPageId(pageId);
        
        // Update the page in Firebase
        return pageRepository.updatePage(containerPage);
    }
}